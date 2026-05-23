package com.uit.vitour.utils;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ImageCompressor.java — Utility for compressing user-selected images before
 * uploading to Firebase Storage.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * WHY THIS EXISTS
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *   A modern phone camera produces 8–15 MB JPEG files at 4000×3000 px.
 *   Uploading that directly:
 *     • Wastes Firebase Storage egress quota (billed per GB downloaded by Glide)
 *     • Makes RecyclerView scroll jank — Glide must decode a large bitmap even
 *       with its disk cache because the source is huge
 *     • Can OOM the device during Bitmap.createScaledBitmap() on low-RAM phones
 *
 *   This class caps images at 1080 × 1080 px and encodes at JPEG 75 %.
 *   Typical result: 3–6 MB → 80–200 KB. No perceptible quality loss for a
 *   RecyclerView thumbnail.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * DECODE STRATEGY — inSampleSize
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *   BitmapFactory.Options.inJustDecodeBounds = true performs a FREE header-only
 *   read that returns the real image dimensions without allocating any pixel
 *   memory. We use those dimensions to calculate an inSampleSize power-of-two
 *   downscale factor BEFORE decoding the full Bitmap.
 *
 *   Example: 4000 × 3000 image, MAX_DIMENSION = 1080
 *     calculated inSampleSize = 4   (4000/1080 ≈ 3.7 → next power of 2 = 4)
 *     decoded Bitmap size     ≈ 1000 × 750 px  (~3 MB heap instead of ~48 MB)
 *
 *   A second pass with createScaledBitmap() then snaps to the exact target size.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * OOM SAFETY
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *   Even with inSampleSize, decoding a Bitmap on a 512 MB device with low free
 *   heap can still OOM. We catch OutOfMemoryError explicitly (not Exception)
 *   and return null. The caller (ReviewRepository) must handle null gracefully.
 *
 * ─────────────────────────────────────────────────────────────────────────────
 * EXIF ROTATION
 * ─────────────────────────────────────────────────────────────────────────────
 *
 *   Android camera apps often store orientation in EXIF metadata rather than
 *   physically rotating pixels. Without correction, portrait photos appear as
 *   landscape in the review list. We read the EXIF orientation tag and apply
 *   a Matrix rotation before re-encoding.
 */
public final class ImageCompressor {

    private static final String TAG = "ImageCompressor";

    // ── Compression settings ─────────────────────────────────────────────────

    /**
     * Maximum width or height of the output image in pixels.
     * At 1080 px, images look sharp in a RecyclerView card while staying small.
     * Change to 720 for even smaller files at the cost of slight quality loss.
     */
    public static final int MAX_DIMENSION = 1080;

    /**
     * JPEG encoding quality (0–100).
     * 75 is the sweet spot: visually lossless for review photos,
     * typically achieves 10–20× size reduction vs. a raw camera file.
     */
    public static final int JPEG_QUALITY = 75;

    /**
     * Hard cap on the output byte array size.
     * If compression still exceeds this after all steps, we reject the image.
     * 2 MB is generous — well-compressed 1080 px JPEGs are usually 100–400 KB.
     */
    public static final int MAX_BYTES = 2 * 1024 * 1024; // 2 MB

    /**
     * Absolute hard limit for input file size to prevent OOM before we even
     * attempt to decode bounds.
     */
    public static final long MAX_INPUT_BYTES = 10 * 1024 * 1024L; // 10 MB

    // Prevent instantiation — static utility class only
    private ImageCompressor() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Compresses the image at {@code uri} to a JPEG byte array suitable for
     * Firebase Storage upload.
     *
     * Steps:
     *   1. Read EXIF orientation from the source URI
     *   2. Decode with inSampleSize to avoid OOM on large originals
     *   3. Scale to MAX_DIMENSION × MAX_DIMENSION (aspect-ratio preserving)
     *   4. Apply EXIF rotation correction
     *   5. Re-encode as JPEG at JPEG_QUALITY
     *   6. Verify output does not exceed MAX_BYTES
     *
     * @param context  Application or Activity context (for ContentResolver)
     * @param uri      Content URI of the image (e.g. from MediaStore gallery picker)
     * @return JPEG-encoded byte array ready for StorageReference.putBytes(),
     *         or {@code null} if compression failed (OOM, IO error, oversize output)
     */
    @Nullable
    public static byte[] compress(@NonNull Context context, @NonNull Uri uri) {
        Log.d(TAG, "compress() — START uri=" + uri);

        // ── Step 0: Pre-check input file size ─────────────────────────────
        long inputSize = getFileSize(context, uri);
        if (inputSize > MAX_INPUT_BYTES) {
            Log.e(TAG, "compress() — Input file too large: " + formatBytes(inputSize)
                    + " (Limit is 10 MB). Rejecting to prevent OOM.");
            return null;
        }

        // ── Step 1: Read EXIF orientation ─────────────────────────────────
        int exifRotation = readExifRotation(context, uri);
        Log.d(TAG, "compress() — EXIF rotation=" + exifRotation + "°");

        // ── Step 2: Decode with inSampleSize ──────────────────────────────
        int[] rawDimensions = readDimensionsWithoutDecoding(context, uri);
        if (rawDimensions == null) {
            Log.e(TAG, "compress() — FAILED to read image dimensions");
            return null;
        }
        int rawWidth  = rawDimensions[0];
        int rawHeight = rawDimensions[1];
        Log.d(TAG, "compress() — original dimensions: " + rawWidth + " × " + rawHeight + " px");

        int sampleSize = calculateInSampleSize(rawWidth, rawHeight, MAX_DIMENSION, MAX_DIMENSION);
        Log.d(TAG, "compress() — computed inSampleSize=" + sampleSize
                + " (sampled ≈" + (rawWidth / sampleSize) + " × " + (rawHeight / sampleSize) + " px)");

        Bitmap sampledBitmap = decodeSampledBitmap(context, uri, sampleSize);
        if (sampledBitmap == null) {
            Log.e(TAG, "compress() — FAILED to decode bitmap (OOM or IO error)");
            return null;
        }
        Log.d(TAG, "compress() — decoded bitmap: "
                + sampledBitmap.getWidth() + " × " + sampledBitmap.getHeight() + " px"
                + " | alloc=" + (sampledBitmap.getByteCount() / 1024) + " KB heap");

        // ── Step 3: Scale to MAX_DIMENSION (aspect-ratio preserving) ──────
        Bitmap scaledBitmap = scaleBitmap(sampledBitmap, MAX_DIMENSION);
        if (scaledBitmap != sampledBitmap) {
            sampledBitmap.recycle(); // free the intermediate bitmap immediately
        }
        Log.d(TAG, "compress() — scaled bitmap: "
                + scaledBitmap.getWidth() + " × " + scaledBitmap.getHeight() + " px");

        // ── Step 4: Apply EXIF rotation ───────────────────────────────────
        Bitmap finalBitmap = applyRotation(scaledBitmap, exifRotation);
        if (finalBitmap != scaledBitmap) {
            scaledBitmap.recycle();
        }

        // ── Step 5: JPEG encode ───────────────────────────────────────────
        byte[] bytes = encodeToJpeg(finalBitmap, JPEG_QUALITY);
        finalBitmap.recycle();

        if (bytes == null) {
            Log.e(TAG, "compress() — FAILED during JPEG encoding (OOM)");
            return null;
        }

        // ── Step 6: Hard size cap ─────────────────────────────────────────
        Log.d(TAG, "compress() — DONE"
                + " | output size: " + formatBytes(bytes.length)
                + " | reduction: " + computeReductionLabel(rawWidth, rawHeight, bytes.length));

        if (bytes.length > MAX_BYTES) {
            Log.e(TAG, "compress() — output " + formatBytes(bytes.length)
                    + " exceeds hard cap of " + formatBytes(MAX_BYTES) + " — rejecting");
            return null;
        }

        return bytes;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Reads width and height from the image header without allocating pixel memory.
     * Returns [width, height] or null on failure.
     */
    @Nullable
    private static int[] readDimensionsWithoutDecoding(@NonNull Context context, @NonNull Uri uri) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return null;
            BitmapFactory.decodeStream(is, null, opts);
        } catch (IOException e) {
            Log.e(TAG, "readDimensions() — IOException: " + e.getMessage());
            return null;
        }

        if (opts.outWidth <= 0 || opts.outHeight <= 0) return null;
        return new int[]{opts.outWidth, opts.outHeight};
    }

    /**
     * Computes the largest power-of-two inSampleSize such that the decoded
     * bitmap fits within reqWidth × reqHeight.
     *
     * Power-of-two values are required by the decoder — arbitrary values are
     * rounded down to the nearest power of two by the platform anyway.
     */
    private static int calculateInSampleSize(int width, int height, int reqWidth, int reqHeight) {
        int sampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth  = width  / 2;
            while ((halfHeight / sampleSize) >= reqHeight
                    && (halfWidth  / sampleSize) >= reqWidth) {
                sampleSize *= 2;
            }
        }
        return sampleSize;
    }

    /**
     * Decodes the bitmap using the pre-calculated inSampleSize.
     * Catches OutOfMemoryError — returns null if allocation fails.
     */
    @Nullable
    private static Bitmap decodeSampledBitmap(@NonNull Context context,
                                               @NonNull Uri uri,
                                               int sampleSize) {
        BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = false;
        opts.inSampleSize       = sampleSize;
        opts.inPreferredConfig  = Bitmap.Config.RGB_565; // 2 bytes/px vs 4 for ARGB_8888
                                                          // safe for opaque photos

        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) {
                Log.e(TAG, "decodeSampledBitmap() — ContentResolver returned null stream");
                return null;
            }
            return BitmapFactory.decodeStream(is, null, opts);
        } catch (IOException e) {
            Log.e(TAG, "decodeSampledBitmap() — IOException: " + e.getMessage());
            return null;
        } catch (OutOfMemoryError oom) {
            // Explicitly catch OOM — it is a real risk when decoding large images
            // on low-memory devices. Log clearly and return null for graceful handling.
            Log.e(TAG, "decodeSampledBitmap() — OutOfMemoryError: " + oom.getMessage()
                    + " | sampleSize was " + sampleSize
                    + " | consider increasing sampleSize or lowering MAX_DIMENSION");
            return null;
        }
    }

    /**
     * Scales {@code bitmap} so its longest side is at most {@code maxDimension}.
     * Maintains aspect ratio. Returns the original bitmap unchanged if it is
     * already within the limit (no unnecessary copy is made).
     */
    @NonNull
    private static Bitmap scaleBitmap(@NonNull Bitmap bitmap, int maxDimension) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        if (w <= maxDimension && h <= maxDimension) {
            return bitmap; // already fits — return original reference, no copy
        }

        float scale     = (float) maxDimension / Math.max(w, h);
        int targetWidth  = Math.round(w * scale);
        int targetHeight = Math.round(h * scale);

        try {
            return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true);
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "scaleBitmap() — OutOfMemoryError during createScaledBitmap: "
                    + oom.getMessage() + " — returning un-scaled bitmap");
            return bitmap; // return best effort rather than crashing
        }
    }

    /**
     * Reads the EXIF orientation tag from the image without decoding pixels.
     * Returns the clockwise rotation angle (0, 90, 180, or 270).
     */
    private static int readExifRotation(@NonNull Context context, @NonNull Uri uri) {
        try (InputStream is = context.getContentResolver().openInputStream(uri)) {
            if (is == null) return 0;
            ExifInterface exif = new ExifInterface(is);
            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:  return 90;
                case ExifInterface.ORIENTATION_ROTATE_180: return 180;
                case ExifInterface.ORIENTATION_ROTATE_270: return 270;
                default:                                   return 0;
            }
        } catch (IOException e) {
            Log.w(TAG, "readExifRotation() — could not read EXIF: " + e.getMessage()
                    + " — assuming 0° rotation");
            return 0;
        }
    }

    /**
     * Rotates {@code bitmap} by {@code degrees} clockwise.
     * Returns the original bitmap if rotation is 0° (no copy).
     */
    @NonNull
    private static Bitmap applyRotation(@NonNull Bitmap bitmap, int degrees) {
        if (degrees == 0) return bitmap;
        try {
            Matrix matrix = new Matrix();
            matrix.postRotate(degrees);
            Bitmap rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
            Log.d(TAG, "applyRotation() — applied " + degrees + "° rotation");
            return rotated;
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "applyRotation() — OutOfMemoryError: " + oom.getMessage()
                    + " — skipping rotation");
            return bitmap;
        }
    }

    /**
     * Encodes {@code bitmap} as a JPEG byte array at the given quality.
     * Returns null on OutOfMemoryError.
     */
    @Nullable
    private static byte[] encodeToJpeg(@NonNull Bitmap bitmap, int quality) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            return baos.toByteArray();
        } catch (OutOfMemoryError oom) {
            Log.e(TAG, "encodeToJpeg() — OutOfMemoryError: " + oom.getMessage());
            return null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Logging helpers
    // ─────────────────────────────────────────────────────────────────────────

    /** Formats a byte count as "X.X KB" or "X.X MB". */
    static String formatBytes(long bytes) {
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024f);
        }
        return String.format("%.2f MB", bytes / (1024f * 1024f));
    }

    /** Estimates the uncompressed size (rawW × rawH × 3 bytes for RGB) and computes ratio. */
    private static String computeReductionLabel(int rawW, int rawH, int outputBytes) {
        long estimatedRawBytes = (long) rawW * rawH * 3; // RGB, no alpha
        float ratio = (float) outputBytes / estimatedRawBytes * 100f;
        return String.format("%.1f%% of estimated raw (%.1f MB raw → %s compressed)",
                ratio, estimatedRawBytes / (1024f * 1024f), formatBytes(outputBytes));
    }

    /**
     * Attempts to query the file size of the given Uri.
     * Returns 0 if unknown.
     */
    private static long getFileSize(@NonNull Context context, @NonNull Uri uri) {
        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (sizeIndex != -1) {
                        return cursor.getLong(sizeIndex);
                    }
                }
            } catch (Exception e) {
                Log.w(TAG, "getFileSize() — failed to query URI: " + e.getMessage());
            }
        } else if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
            return new java.io.File(uri.getPath()).length();
        }
        return 0;
    }
}
