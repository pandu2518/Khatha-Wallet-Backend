package com.khathabook.service;

import com.khathabook.model.Bill;
import com.khathabook.model.Customer;
import com.khathabook.model.Retailer;
import com.khathabook.repository.CustomerRepository;
import com.khathabook.repository.RetailerRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.List;

@Service
public class NotificationService {

    private final CustomerRepository customerRepo;
    private final RetailerRepository retailerRepo;
    private final RestTemplate restTemplate;

    @Value("${brevo.api.key}")
    private String brevoApiKey;

    @Value("${brevo.sender.email}")
    private String senderEmail;

    public NotificationService(
            CustomerRepository customerRepo,
            RetailerRepository retailerRepo
    ) {
        this.customerRepo = customerRepo;
        this.retailerRepo = retailerRepo;
        this.restTemplate = new RestTemplate();
    }

    private void sendEmailViaBrevo(String toEmail, String subject, String textContent) {
        if (brevoApiKey == null || brevoApiKey.isBlank()) {
            System.out.println("⚠️ BREVO_API_KEY is not set. Skipping email to: " + toEmail);
            return;
        }

        try {
            String url = "https://api.brevo.com/v3/smtp/email";
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", brevoApiKey);

            // Brevo expects a specific JSON structure
            Map<String, Object> body = Map.of(
                "sender", Map.of("name", "KhathaBook", "email", senderEmail),
                "to", List.of(Map.of("email", toEmail)),
                "subject", subject,
                "htmlContent", textContent.replace("\n", "<br>")
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            restTemplate.postForEntity(url, request, String.class);
            
            System.out.println("✅ Email Sent Successfully via Brevo to: " + toEmail);
        } catch (Exception e) {
            System.err.println("❌ Brevo Email Failed: " + e.getMessage());
        }
    }

    // ======================================================
    // ✅ OTP EMAIL
    // ======================================================
    public void sendOtpEmail(String toEmail, String otp) {
        System.out.println("⚠️ [DEV MODE] OTP for " + toEmail + " is: " + otp);
        String html = """
                Hello,
                
                Your OTP is: <strong>%s</strong>
                
                Valid for 5 minutes.
                
                - KhathaBook
                """.formatted(otp);
        sendEmailViaBrevo(toEmail, "KhathaBook Login OTP", html);
    }

    // ======================================================
    // ✅ BILL EMAIL
    // ======================================================
    public void sendBillEmail(Bill bill, Long retailerId) {
        try {
            if (bill.getCustomer() == null) return;

            Customer customer = customerRepo.findById(bill.getCustomer().getId()).orElse(null);
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) return;

            Retailer retailer = retailerRepo.findById(retailerId).orElse(null);
            if (retailer == null) return;

            String html = """
                    Hello %s,
                    
                    Here are your bill details:
                    
                    🧾 Bill No: %s
                    📅 Date: %s
                    🛒 Items: %s
                    💰 Total: ₹ %.2f
                    💵 Paid: ₹ %.2f
                    ⏳ Due: ₹ %.2f
                    📌 Status: %s
                    
                    Regards,
                    %s
                    KhathaBook
                    """.formatted(
                    customer.getName(),
                    bill.getBillNumber(),
                    bill.getBillDate(),
                    bill.getItems(),
                    bill.getAmount(),
                    bill.getPaidAmount(),
                    bill.getDueAmount(),
                    bill.getStatus(),
                    retailer.getEmail()
            );

            sendEmailViaBrevo(customer.getEmail(), "Bill Details from KhathaBook", html);
        } catch (Exception e) {
            System.out.println("Bill email failed: " + e.getMessage());
        }
    }

    // ======================================================
    // ✅ DUE REMINDER EMAIL
    // ======================================================
    public void sendDueAmountEmail(Long customerId, Long retailerId) {
        try {
            Customer customer = customerRepo.findById(customerId).orElse(null);
            if (customer == null || customer.getEmail() == null || customer.getEmail().isBlank()) return;

            Retailer retailer = retailerRepo.findById(retailerId).orElse(null);
            if (retailer == null) return;

            String html = """
                    Hello %s,
                    
                    Pending Due Amount: ₹ %.2f
                    
                    Please clear the payment at your convenience.
                    
                    Regards,
                    %s
                    """.formatted(
                    customer.getName(),
                    customer.getDueAmount(),
                    retailer.getEmail()
            );
            sendEmailViaBrevo(customer.getEmail(), "Payment Reminder – KhathaBook", html);
        } catch (Exception e) {
            System.out.println("Due reminder email failed: " + e.getMessage());
        }
    }

    // ======================================================
    // ✅ NEW ORDER EMAIL (To Retailer)
    // ======================================================
    public void sendNewOrderEmail(com.khathabook.model.Order order, Retailer retailer) {
        try {
            if (retailer == null || retailer.getEmail() == null) return;

            String html = """
                    New Order Alert!
                    
                    Order #%d from %s
                    Total: ₹ %.2f
                    
                    Items:
                    %s
                    
                    Please check your dashboard to process this order.
                    """.formatted(
                    order.getId(),
                    order.getCustomer().getName(),
                    order.getTotalAmount(),
                    order.getItems()
            );
            sendEmailViaBrevo(retailer.getEmail(), "New Order Received! 📦", html);
        } catch (Exception e) {
            System.err.println("❌ New Order Email Failed: " + e.getMessage());
        }
    }

    // ======================================================
    // ✅ ORDER STATUS EMAIL (To Customer)
    // ======================================================
    public void sendOrderStatusEmail(com.khathabook.model.Order order, Customer customer) {
        try {
            if (customer == null || customer.getEmail() == null) return;

            String statusEmoji = switch (order.getStatus()) {
                case "PACKED" -> "📦";
                case "READY" -> "✅";
                case "DELIVERED" -> "🎉";
                case "COMPLETED" -> "🎉";
                default -> "ℹ️";
            };

            String extraMessage = "";
            if ("READY".equals(order.getStatus())) {
                extraMessage = "Your order is ready for pickup! Please visit the store.";
            } else if ("PACKED".equals(order.getStatus())) {
                String otp = order.getDeliveryOtp() != null ? order.getDeliveryOtp() : "N/A";
                extraMessage = "We have packed your items. \n\n🔐 **Your Delivery OTP is: " + otp + "**\n\nPlease share this OTP with the delivery agent.";
            }

            String html = """
                    Hello %s,
                    
                    Your order #%d is now %s!
                    
                    Status: %s %s
                    
                    %s
                    
                    - KhathaBook
                    """.formatted(
                    customer.getName(),
                    order.getId(),
                    order.getStatus(),
                    order.getStatus(),
                    statusEmoji,
                    extraMessage
            );

            sendEmailViaBrevo(customer.getEmail(), "Order Update: " + order.getStatus() + " " + statusEmoji, html);
        } catch (Exception e) {
            System.err.println("❌ Order Status Email Failed: " + e.getMessage());
        }
    }
}
