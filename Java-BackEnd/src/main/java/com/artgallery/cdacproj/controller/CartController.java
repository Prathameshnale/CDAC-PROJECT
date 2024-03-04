package com.artgallery.cdacproj.controller;

import com.artgallery.cdacproj.model.Cart;
import com.artgallery.cdacproj.model.User;
import com.artgallery.cdacproj.service.CartService;
import com.artgallery.cdacproj.service.UserException;
import com.artgallery.cdacproj.service.UserService;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.razorpay.*;

@RestController
@RequestMapping("/api/carts")
public class CartController {

    private CartService cartService;
    private UserService userService;

    @Autowired
    public CartController(CartService cartService, UserService userService) {
        this.cartService = cartService;
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    public ResponseEntity<?> getCartByUserId(@PathVariable Long userId, Authentication authentication) {
        // Check if authenticated user is the same as the requested user
        if (!userId.equals(getUserIdFromAuthentication(authentication))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Cart cart = cartService.findByUserId(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/{userId}/art/{artId}")
    public ResponseEntity<?> addProductToCart(@PathVariable Long userId, @PathVariable Long artId, Authentication authentication) {
        if (!userId.equals(getUserIdFromAuthentication(authentication))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Cart cart = cartService.addToCart(userId, artId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{userId}/art/{artId}")
    public ResponseEntity<?> removeProductFromCart(@PathVariable Long userId, @PathVariable Long artId, Authentication authentication) {
        // Check if authenticated user is the same as the requested user
        if (!userId.equals(getUserIdFromAuthentication(authentication))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Cart cart = cartService.removeFromCart(userId, artId);
        return ResponseEntity.ok(cart);
    }

    @DeleteMapping("/{userId}/clear")
    public ResponseEntity<?> clearCart(@PathVariable Long userId, Authentication authentication) {
        // Check if authenticated user is the same as the requested user
        if (!userId.equals(getUserIdFromAuthentication(authentication))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        Cart cart = cartService.clearCart(userId);
        return ResponseEntity.ok(cart);
    }

    @PostMapping("/{userId}/payment")
    public ResponseEntity<?> processPayment(@PathVariable Long userId, Authentication authentication) {
        // Check if authenticated user is the same as the requested user
        if (!userId.equals(getUserIdFromAuthentication(authentication))) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Unauthorized access");
        }
        try {
            // Get cart details
            Cart cart = cartService.findByUserId(userId);

            // Calculate total amount
            cart.updateTotal();
            long totalAmount = (long) (cart.getTotal() * 100); // Convert to paisa

            // Create a Razorpay order
            RazorpayClient razorpay = new RazorpayClient("rzp_test_oimo0nzZmKum31", "yGtCAQ3m9vqMsHSb7mJiBrDs");
            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", totalAmount);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", UUID.randomUUID().toString());
            Order order = razorpay.Orders.create(orderRequest);


            Map<String, String> paymentDetails = new HashMap<>();
            paymentDetails.put("orderId", order.get("id"));
            paymentDetails.put("amount", String.valueOf(totalAmount));
            // Add other necessary details here
            return ResponseEntity.ok(paymentDetails);
        } catch (RazorpayException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to create Razorpay order: " + e.getMessage());
        }
    }

    // Helper method to get user ID from authentication object
    private Long getUserIdFromAuthentication(Authentication authentication) {
        String email = authentication.getName();
        try {
            User user = userService.findUserByEmail(email);
            if (user != null) {
                return user.getId(); // Assuming user.getId() returns the user's ID
            } else {
                // User not found in the database
                return null; // Or handle the scenario according to your application's logic
            }
        } catch (UserException e) {
            // Handle exception, e.g., log and return null
            e.printStackTrace();
            return null;
        }
    }
}
