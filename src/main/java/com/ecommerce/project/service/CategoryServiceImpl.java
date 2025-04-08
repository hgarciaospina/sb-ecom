package com.ecommerce.project.service;

import com.ecommerce.project.exception.DuplicateValueException;
import com.ecommerce.project.exception.EntityNotFoundException;
import com.ecommerce.project.exception.InvalidLengthException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.payload.CategoryDTO;
import com.ecommerce.project.payload.CategoryResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ModelMapper modelMapper;


    @Override
    public CategoryResponse getAllCategories() {
        List<CategoryDTO> categoryDTOS = categoryRepository.findAll().stream()
                .map(category -> modelMapper.map(category, CategoryDTO.class))
                .toList();

        if (categoryDTOS.isEmpty()) {
            throw new EntityNotFoundException("No categories available.");
        }

        return new CategoryResponse(categoryDTOS);
    }

    @Override
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        if (categoryDTO.getCategoryName() == null || categoryDTO.getCategoryName().trim().isEmpty()) {
            throw new InvalidLengthException("Category name cannot be empty.");
        }

        if (categoryDTO.getCategoryName().length() < 5) {
            throw new InvalidLengthException("Category name must be at least 5 characters long.");
        }

        if (categoryRepository.findByCategoryName(categoryDTO.getCategoryName()) != null){
            throw new DuplicateValueException("Category with the name " + categoryDTO.getCategoryName() + " already exists !!!");
        }

        Category category = modelMapper.map(categoryDTO, Category.class);

        Category savedCategory = categoryRepository.save(category);

        return modelMapper.map(savedCategory, CategoryDTO.class);
    }

    @Override
    public CategoryDTO updateCategory(CategoryDTO categoryDTO, Long categoryId) {
        if (categoryDTO.getCategoryName() == null || categoryDTO.getCategoryName().trim().isEmpty()) {
            throw new InvalidLengthException("Category name cannot be empty.");
        }

        if (categoryDTO.getCategoryName().trim().length() < 5) {
            throw new InvalidLengthException("Category name must be at least 5 characters long.");
        }

        return categoryRepository.findById(categoryId)
                .map(existingCategory -> {
                    existingCategory.setCategoryName(categoryDTO.getCategoryName());
                    return modelMapper.map(categoryRepository.save(existingCategory), CategoryDTO.class);
                })
                .orElseThrow(() -> new EntityNotFoundException("Category with ID " + categoryId + " not found"));
    }

    @Override
    public void deleteCategory(Long categoryId) {
        categoryRepository.findById(categoryId)
                .ifPresentOrElse(
                        category -> categoryRepository.delete(category),
                        () -> { throw new EntityNotFoundException("Category with categoryId: " + categoryId + " not found."); }
                );
    }
}