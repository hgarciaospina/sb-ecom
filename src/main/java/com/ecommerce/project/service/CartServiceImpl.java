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
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service implementation class for managing cart operations such as adding products to a cart,
 * retrieving all carts, and fetching a specific user's cart by ID and email.
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
     * @param quantity  the quantity of the product to add
     * @return the updated cart as a DTO
     * @throws ResourceNotFoundException if the product does not exist or is unavailable
     * @throws APIException              if the product already exists in the cart or quantity is insufficient
     */
    @Override
    public CartDTO addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItem cartItem = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);

        if (cartItem != null) {
            throw new APIException("Product " + product.getProductName() + " already exists in the cart");
        }

        if (product.getQuantity() == 0) {
            throw new ResourceNotFoundException(product.getProductName() + " is not available");
        }

        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName()
                    + " less than or equal to the quantity " + product.getQuantity() + ".");
        }

        CartItem newCartItem = new CartItem();
        newCartItem.setProduct(product);
        newCartItem.setCart(cart);
        newCartItem.setQuantity(quantity);
        newCartItem.setDiscount(product.getDiscount());
        newCartItem.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItem);

        product.setQuantity(product.getQuantity()); // May be redundant unless side-effects exist

        cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<CartItem> cartItems = cart.getCartItems();

        Stream<ProductDTO> productStream = cartItems.stream().map(item -> {
            ProductDTO productDTO = modelMapper.map(item.getProduct(), ProductDTO.class);
            productDTO.setQuantity(item.getQuantity());
            return productDTO;
        });

        cartDTO.setProducts(productStream.toList());

        return cartDTO;
    }

    /**
     * Retrieves a list of all existing carts.
     *
     * @return a list of cart DTOs
     * @throws ResourceNotFoundException if no carts are found
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
     * Retrieves a specific cart for a user based on their email and the cart ID.
     *
     * @param emailId the email address of the user
     * @param cartId  the ID of the cart to retrieve
     * @return the corresponding cart DTO
     * @throws ResourceNotFoundException if the cart is not found
     */
    @Override
    public CartDTO getCart(String emailId, Long cartId) {
        Cart cart = Optional.ofNullable(cartRepository.findCartByEmailAndCartId(emailId, cartId))
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> productDTOs = cart.getCartItems().stream()
                .map(cartItem -> {
                    Product product = cartItem.getProduct();
                    product.setQuantity(cartItem.getQuantity());
                    return modelMapper.map(product, ProductDTO.class);
                })
                .toList();

        cartDTO.setProducts(productDTOs);
        return cartDTO;
    }

    /**
     * Converts a Cart entity into a CartDTO, mapping its products.
     *
     * @param cart the Cart entity to convert
     * @return the corresponding CartDTO
     */
    private CartDTO convertToCartDTO(Cart cart) {
        CartDTO cartDTO = modelMapper.map(cart, CartDTO.class);

        List<ProductDTO> productsDTO = cart.getCartItems().stream()
                .map(item -> modelMapper.map(item.getProduct(), ProductDTO.class))
                .toList();

        cartDTO.setProducts(productsDTO);
        return cartDTO;
    }

    /**
     * Creates a new cart for the currently logged-in user if one does not already exist.
     *
     * @return the existing or newly created Cart entity
     */
    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return userCart;
        }

        Cart cart = new Cart();
        cart.setTotalPrice(0.00);
        cart.setUser(authUtil.loggedInUser());

        return cartRepository.save(cart);
    }
}