package com.uit.vitour.chat.ui;

import android.widget.TextView;
// import io.noties.markwon.Markwon;

public class MarkdownHelper {
    
    // private static Markwon markwon;

    public static void setMarkdownText(TextView textView, String markdownText) {
        /*
        if (markwon == null) {
            markwon = Markwon.builder(textView.getContext())
                    // add plugins here like corePlugin, imagePlugin, etc.
                    .build();
        }
        markwon.setMarkdown(textView, markdownText);
        */
        
        // Fallback for now without Markwon dependency
        textView.setText(markdownText);
    }
}
