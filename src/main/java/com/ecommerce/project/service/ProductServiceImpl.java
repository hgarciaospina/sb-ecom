package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.*;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.*;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * Service implementation for managing product operations such as
 * adding, updating, deleting products and performing searches.
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartService cartService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path;

    /**
     * Adds a new product to a specific category.
     */
    @Override
    public ProductDTO addProduct(Long categoryId, ProductDTO productDTO) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        validateProductData(productDTO);

        if (productRepository.findByProductName(productDTO.getProductName()) != null) {
            throw new DuplicateValueException("Product with the name " + productDTO.getProductName() + " already exists!");
        }

        Product product = modelMapper.map(productDTO, Product.class);
        product.setImage("default.png");
        product.setCategory(category);
        product.setSpecialPrice(calculateSpecialPrice(product));
        Product savedProduct = productRepository.save(product);

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    /**
     * Retrieves all products with pagination and sorting.
     */
    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSort(sortBy, sortOrder));
        Page<Product> productPage = productRepository.findAll(pageable);

        if (productPage.isEmpty()) {
            throw new ResourceNotFoundException("No products available!");
        }

        return mapToProductResponse(productPage);
    }

    /**
     * Retrieves products by category with pagination and sorting.
     */
    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSort(sortBy, sortOrder));
        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageable);

        if (productPage.isEmpty()) {
            throw new APIException("No products available with category " + category.getCategoryName() + "!");
        }

        return mapToProductResponse(productPage);
    }

    /**
     * Searches products by keyword in product name.
     */
    @Override
    public ProductResponse searchByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new InvalidLengthException("Keyword cannot be empty!");
        }

        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSort(sortBy, sortOrder));
        Page<Product> productPage = productRepository.findByProductNameLikeIgnoreCase('%' + keyword + '%', pageable);

        if (productPage.isEmpty()) {
            throw new ResourceNotFoundException("No products available with keyword " + keyword + "!");
        }

        return mapToProductResponse(productPage);
    }

    /**
     * Updates an existing product by ID.
     */
    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        Product updatedProduct = modelMapper.map(productDTO, Product.class);
        updateProductFields(productFromDb, updatedProduct);

        Product savedProduct = productRepository.save(productFromDb);

        cartRepository.findCartsByProductId(productId).forEach(cart ->
                cartService.updateProductInCarts(cart.getCartId(), productId));

        return modelMapper.map(savedProduct, ProductDTO.class);
    }

    /**
     * Deletes a product and removes it from all carts.
     */
    @Override
    public ProductDTO deleteProduct(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        cartRepository.findCartsByProductId(productId)
                .forEach(cart -> cartService.deleteProductFromCart(cart.getCartId(), productId));

        productRepository.delete(product);

        return modelMapper.map(product, ProductDTO.class);
    }

    /**
     * Updates a product's image file.
     */
    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        String fileName = fileService.uploadImage(path, image);
        productFromDb.setImage(fileName);

        Product updatedProduct = productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    // ----------- PRIVATE HELPERS ------------

    /**
     * Validates product name and description.
     */
    private void validateProductData(ProductDTO productDTO) {
        if (productDTO.getProductName() == null || productDTO.getProductName().trim().isEmpty()) {
            throw new InvalidLengthException("Product name cannot be empty!");
        }
        if (productDTO.getProductName().length() < 5) {
            throw new InvalidLengthException("Product name must be at least 5 characters long!");
        }

        if (productDTO.getDescription() == null || productDTO.getDescription().trim().isEmpty()) {
            throw new InvalidLengthException("Product description cannot be empty!");
        }
        if (productDTO.getDescription().length() < 5) {
            throw new InvalidLengthException("Product description must be at least 5 characters long!");
        }
    }

    /**
     * Calculates special price based on discount.
     */
    private static double calculateSpecialPrice(Product product) {
        return product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
    }

    /**
     * Returns sort object based on field and direction.
     */
    private Sort getSort(String sortBy, String sortOrder) {
        return sortOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    }

    /**
     * Maps a page of products to a ProductResponse.
     */
    private ProductResponse mapToProductResponse(Page<Product> pageProducts) {
        List<ProductDTO> productDTOs = pageProducts.getContent().stream()
                .map(product -> modelMapper.map(product, ProductDTO.class))
                .toList();

        return new ProductResponse(
                productDTOs,
                pageProducts.getNumber(),
                pageProducts.getSize(),
                pageProducts.getTotalElements(),
                pageProducts.getTotalPages(),
                pageProducts.isLast()
        );
    }

    /**
     * Updates product fields.
     */
    private void updateProductFields(Product original, Product updated) {
        original.setProductName(updated.getProductName());
        original.setDescription(updated.getDescription());
        original.setQuantity(updated.getQuantity());
        original.setDiscount(updated.getDiscount());
        original.setPrice(updated.getPrice());
        original.setSpecialPrice(calculateSpecialPrice(updated));
    }
}
