package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.*;
import com.ecommerce.project.model.*;
import com.ecommerce.project.payload.*;
import com.ecommerce.project.repositories.*;
import com.ecommerce.project.util.AuthUtil;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Implementation of {@link ProductService} that manages product operations such as:
 * - Adding, updating, and deleting products
 * - Searching products by keyword or category
 * - Updating product images with cleanup
 * - Tracking price changes with history
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

    @Autowired
    private AuthUtil authUtil;

    @Autowired
    private PriceHistoryService priceHistoryService;

    @Value("${project.image}")
    private String path;

    @Value("${image.base.url}")
    private String imageBaseUrl;

    /**
     * Adds a new product under the specified category.
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
     * Retrieves a paginated and sorted list of all products.
     */
    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder, String keyword, String category) {
        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSort(sortBy, sortOrder));
        Specification<Product> spec = Specification.where(null);
        if(keyword != null && !keyword.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("productName")), "%" + keyword.toLowerCase() + "%"));
        }

        if(category != null && !category.isEmpty()) {
            spec = spec.and((root, query, criteriaBuilder) ->
                    criteriaBuilder.like(root.get("category").get("categoryName"), category));
        }

        Page<Product> productsPage = productRepository.findAll(spec, pageable);

        if (productsPage.isEmpty()) {
            throw new ResourceNotFoundException("No products available!");
        }

        return mapToProductResponse(productsPage);
    }

    /**
     * Retrieves a paginated list of products by category.
     */
    @Override
    public ProductResponse searchByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Pageable pageable = PageRequest.of(pageNumber, pageSize, getSort(sortBy, sortOrder));
        Page<Product> productPage = productRepository.findByCategoryOrderByPriceAsc(category, pageable);

        if (productPage.isEmpty()) {
            throw new APIException("No products available in category " + category.getCategoryName() + "!");
        }

        return mapToProductResponse(productPage);
    }

    /**
     * Searches for products using a keyword match on the product name.
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
     * Updates an existing product and logs any price changes to the history.
     */
    @Override
    public ProductDTO updateProduct(Long productId, ProductDTO productDTO) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        double oldPrice = productFromDb.getPrice();

        updateProductFields(productFromDb, productDTO);

        if (Double.compare(oldPrice, productFromDb.getPrice()) != 0) {
            PriceHistory priceHistory = new PriceHistory();
            priceHistory.setProduct(productFromDb);
            priceHistory.setOldPrice(oldPrice);
            priceHistory.setNewPrice(productFromDb.getPrice());
            priceHistory.setChangedAt(LocalDateTime.now());
            priceHistory.setChangedBy(authUtil.loggedInUser());

            priceHistoryService.savePriceHistory(priceHistory);
        }

        Product savedProduct = productRepository.save(productFromDb);

        cartRepository.findCartsByProductId(productId).forEach(cart ->
                cartService.updateProductInCarts(cart.getCartId(), productId)
        );

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
     * Updates the product's image: deletes the previous one and uploads a new one with a random name.
     *
     * @param productId the ID of the product
     * @param image     the new image file
     * @return updated ProductDTO
     * @throws IOException if upload or deletion fails
     */
    @Override
    public ProductDTO updateProductImage(Long productId, MultipartFile image) throws IOException {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        // Delete previous image if not default
        String oldImage = productFromDb.getImage();
        fileService.deleteImage(path, oldImage);

        // Upload new image with random name
        String newImageName = fileService.uploadImage(path, image);
        productFromDb.setImage(newImageName);

        Product updatedProduct = productRepository.save(productFromDb);
        return modelMapper.map(updatedProduct, ProductDTO.class);
    }

    // ----------------- PRIVATE HELPERS -----------------

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

    private static double calculateSpecialPrice(Product product) {
        return product.getPrice() - ((product.getDiscount() * 0.01) * product.getPrice());
    }

    private Sort getSort(String sortBy, String sortOrder) {
        return sortOrder.equalsIgnoreCase("asc") ?
                Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
    }

    private ProductResponse mapToProductResponse(Page<Product> pageProducts) {
        List<ProductDTO> productDTOs = pageProducts.getContent().stream()
                .map(product -> {
                    ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);
                    productDTO.setImage(constructImageUrl(product.getImage()));
                    return productDTO;
                })
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

    private void updateProductFields(Product product, ProductDTO dto) {
        product.setProductName(dto.getProductName());
        product.setDescription(dto.getDescription());
        product.setStock(dto.getStock());
        product.setDiscount(dto.getDiscount());
        product.setPrice(dto.getPrice());
        product.setSpecialPrice(calculateSpecialPrice(product));
    }

    private String constructImageUrl(String imageName) {
        return imageBaseUrl.endsWith("/") ? imageBaseUrl + imageName : imageBaseUrl + "/" + imageName;
    }
}
