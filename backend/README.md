# ViTour Backend

## HOW TO RUN REAL PAYMENT DEMO

1. **Start Spring Boot**: Run the backend application locally on port 8080.
2. **Start Cloudflare Tunnel**: Expose your local port 8080 to the internet using Cloudflare Tunnel or ngrok.
   ```bash
   cloudflared tunnel --url http://localhost:8080
   ```
3. **Configure SePay webhook**: In the SePay Dashboard, set your Webhook URL to point to your tunnel:
   `https://<YOUR_TUNNEL_URL>/api/payment/sepay/webhook`
4. **Open Android app**: Launch the ViTour Android app and initiate a booking.
5. **Scan QR using banking app**: Open your real banking app and scan the generated VietQR code.
6. **Transfer real money**: The amount and content (`BOOKING_{id}`) will be pre-filled. Complete the transfer.
7. **Firestore updates automatically**: SePay will hit the webhook. The backend will verify the transfer and update the Firestore booking document's `paymentStatus` to `PAID` and `status` to `CONFIRMED`.
8. **Android auto confirms booking**: The Android app's realtime Firestore listener will detect the change, auto-dismiss the QR dialog, and show the success screen.
