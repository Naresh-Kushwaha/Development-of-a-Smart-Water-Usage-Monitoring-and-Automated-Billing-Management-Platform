import api from './api.js';

const CHECKOUT_URL = 'https://checkout.razorpay.com/v1/checkout.js';

/** Loads Razorpay's checkout script once, only when somebody actually clicks "Pay now". */
export function loadCheckout() {
  return new Promise((resolve, reject) => {
    if (window.Razorpay) return resolve();
    const s = document.createElement('script');
    s.src = CHECKOUT_URL;
    s.onload = () => resolve();
    s.onerror = () => reject(new Error('Could not load Razorpay Checkout. Check your internet connection.'));
    document.body.appendChild(s);
  });
}

/**
 * Pay one invoice:
 *  1. ask OUR backend to create the order (the amount is decided on the server, not here)
 *  2. open Razorpay's popup
 *  3. on success send Razorpay's three values to OUR backend, which checks the signature and marks the invoice paid
 * Resolves with the updated invoice; rejects with an Error when cancelled/failed.
 */
export async function payInvoice(invoiceId, user) {
  await loadCheckout();
  const { data: o } = await api.post('/payments/orders', { invoiceId });

  return new Promise((resolve, reject) => {
    const rzp = new window.Razorpay({
      key: o.keyId,                // public key id
      order_id: o.orderId,
      amount: o.amountPaise,
      currency: o.currency,
      name: 'AquaTrack',
      description: o.description,
      prefill: { name: user.fullName, email: user.email },
      theme: { color: '#0e7490' },
      handler: async (resp) => {
        try {
          const { data } = await api.post('/payments/verify', {
            orderId: resp.razorpay_order_id,
            paymentId: resp.razorpay_payment_id,
            signature: resp.razorpay_signature,
          });
          resolve(data);
        } catch (e) { reject(e); }
      },
      modal: { ondismiss: () => reject(new Error('Payment cancelled')) },
    });
    rzp.on('payment.failed', (r) => reject(new Error(r?.error?.description || 'Payment failed')));
    rzp.open();
  });
}
