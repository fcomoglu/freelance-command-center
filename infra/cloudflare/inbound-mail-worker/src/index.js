/**
 * Email Worker of the inbound mail domain (docs/adr/0016): posts every message, unchanged, to the API's inbound mail
 * webhook, which parses and files it. API_URL and INBOUND_MAIL_SECRET are Worker secrets.
 */

// The API refuses larger mails; rejecting here tells the sender instead of failing silently later.
const MAX_SIZE = 5 * 1024 * 1024;

export default {
  async email(message, env) {
    if (message.rawSize > MAX_SIZE) {
      message.setReject('Message too large');
      return;
    }
    const response = await fetch(new URL('/api/v1/inbound-mail', env.API_URL), {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${env.INBOUND_MAIL_SECRET}`,
        'X-Envelope-To': message.to,
        'Content-Type': 'message/rfc822',
      },
      body: await new Response(message.raw).arrayBuffer(),
    });
    if (response.ok) return;
    // A refused mail (unreadable, wrong secret) is refused again on every retry.
    if (response.status < 500) {
      message.setReject(`Not accepted (${response.status})`);
      return;
    }
    // The API is down or restarting. Not rejected: the failure shows in the Worker logs, and the user still has the
    // mail in their own mailbox to forward again.
    throw new Error(`Inbound mail webhook answered ${response.status}`);
  },
};
