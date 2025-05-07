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
 * Service implementation class for managing cart operations such as
 * adding products to the cart, retrieving carts, and updating quantities.
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
     *
     * @param productId the ID of the product to add
     * @param quantity  the quantity to add
     * @return the updated cart as a DTO
     * @throws ResourceNotFoundException if the product does not exist or is unavailable
     * @throws APIException              if the product is already in the cart or quantity exceeds availability
     */
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem existingItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (existingItem != null) {
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

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);

        return convertToCartDTO(cart);
    }

    /**
     * Retrieves a list of all carts in the system.
     *
     * @return a list of CartDTOs
     * @throws ResourceNotFoundException if no carts exist
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
     * Retrieves a specific cart for a user based on email and cart ID.
     *
     * @param emailId the user's email
     * @param cartId  the cart ID
     * @return the corresponding CartDTO
     * @throws ResourceNotFoundException if the cart is not found
     */
    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = Optional.ofNullable(cartRepository.findCartByEmailAndCartId(emailId, cartId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        return convertToCartDTO(cart);
    }

    /**
     * Updates the quantity of a product in the logged-in user's cart.
     *
     * @param productId the ID of the product
     * @param quantity  the quantity to add
     * @return the updated CartDTO
     * @throws ResourceNotFoundException if the cart or product is not found
     * @throws APIException              if the product is not in the cart or quantity exceeds availability
     */
    @Transactional
    @Override
    public CartDTO updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();
        Cart cart = Optional.ofNullable(cartRepository.findCartByEmail(emailId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "email", emailId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        validateProductAvailability(product, quantity);

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItem == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart!!!");
        }

        int updatedQuantity = cartItem.getQuantity() + quantity;
        cartItem.setQuantity(updatedQuantity);
        cartItem.setProductPrice(product.getSpecialPrice());
        cartItem.setDiscount(product.getDiscount());

        if (updatedQuantity == 0) {
            cartItemRepository.deleteById(cartItem.getCartItemId());
        } else {
            cartItemRepository.save(cartItem);
        }

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);

        return convertToCartDTO(cart);
    }

    /**
     * Converts a Cart entity to a CartDTO including product list with quantities.
     *
     * @param cart the Cart entity
     * @return the mapped CartDTO
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
     * Creates a new cart for the logged-in user if none exists.
     *
     * @return the existing or new Cart
     */
    private Cart createCart() {
        return Optional.ofNullable(cartRepository.findCartByEmail(authUtil.loggedInEmail()))
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setTotalPrice(0.00);
                    cart.setUser(authUtil.loggedInUser());
                    return cartRepository.save(cart);
                });
    }

    /**
     * Validates that a product is available and the requested quantity is allowed.
     *
     * @param product  the product to validate
     * @param quantity the desired quantity
     * @throws APIException if product is out of stock or quantity exceeds availability
     */
    private void validateProductAvailability(Product product, Integer quantity) {
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }
    }
}