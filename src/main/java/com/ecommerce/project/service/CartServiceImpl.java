package com.ecommerce.project.service;

import com.ecommerce.project.exception.APIException;
import com.ecommerce.project.exception.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItem;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDTO;
import com.ecommerce.project.payload.ProductDTO;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.util.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * Service implementation for managing cart operations.
 */
@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ModelMapper modelMapper;

    /**
     * Adds a product to the currently logged-in user's cart.
     */
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = getOrCreateUserCart();

        Product product = findProductById(productId);

        if (cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId) != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }

        validateProductAvailability(product, quantity);

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        updateCartTotal(cart, product.getSpecialPrice() * quantity);

        return convertToCartDTO(cart);
    }

    /**
     * Retrieves all existing carts in the system.
     */
    @Override
    public List<CartDTO> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();

        if (carts.isEmpty()) {
            throw new ResourceNotFoundException("No cart exists");
        }

        return carts.stream()
                .map(this::convertToCartDTO)
                .toList();
    }

    /**
     * Retrieves a cart for a specific user based on email and cart ID.
     */
    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = Optional.ofNullable(cartRepository.findCartByEmailAndCartId(emailId, cartId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        return convertToCartDTO(cart);
    }

    /**
     * Updates the quantity of a product in the logged-in user's cart.
     */
    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();

        Cart cart = Optional.ofNullable(cartRepository.findCartByEmail(emailId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "email", emailId));

        Product product = findProductById(productId);
        validateProductAvailability(product, quantity);

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        int updatedQuantity = cartItem.getQuantity() + quantity;
        if (updatedQuantity <= 0) {
            cartItemRepository.deleteById(cartItem.getCartItemId());
        } else {
            cartItem.setQuantity(updatedQuantity);
            cartItem.setProductPrice(product.getSpecialPrice());
            cartItem.setDiscount(product.getDiscount());
            cartItemRepository.save(cartItem);
        }

        updateCartTotal(cart, product.getSpecialPrice() * quantity);

        return convertToCartDTO(cart);
    }

    /**
     * Removes a product from the specified cart.
     */
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);

        if (cartItem == null) {
            throw new ResourceNotFoundException("Product", "productId", productId);
        }

        updateCartTotal(cart, -(cartItem.getProductPrice() * cartItem.getQuantity()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId, productId);

        return "Product " + cartItem.getProduct().getProductName() + " removed from the cart !!!";
    }

    // ---------- PRIVATE HELPERS ----------

    /**
     * Retrieves or creates a cart for the logged-in user.
     */
    private Cart getOrCreateUserCart() {
        Cart cart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (cart != null) {
            return cart;
        }

        Cart newCart = new Cart();
        newCart.setTotalPrice(0.00);
        newCart.setUser(authUtil.loggedInUser());
        return cartRepository.save(newCart);
    }

    /**
     * Converts a Cart entity into its corresponding DTO.
     */
    private CartDTO convertToCartDTO(Cart cart) {
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> products = cart.getCartItems().stream()
                .map(item -> {
                    ProductDTO dto = modelMapper.map(item.getProduct(), ProductDTO.class);
                    dto.setQuantity(item.getQuantity());
                    return dto;
                })
                .toList();

        cartDTO.setProducts(products);
        return cartDTO;
    }

    /**
     * Validates if the product can be added to the cart in the desired quantity.
     */
    private void validateProductAvailability(Product product, Integer quantity) {
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please order " + product.getProductName()
                    + " in a quantity less than or equal to " + product.getQuantity());
        }
    }

    /**
     * Fetches a product by ID or throws an exception if not found.
     */
    private Product findProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));
    }

    /**
     * Updates the total price of the cart and persists it.
     */
    private void updateCartTotal(Cart cart, double priceChange) {
        cart.setTotalPrice(cart.getTotalPrice() + priceChange);
        cartRepository.save(cart);
    }
}