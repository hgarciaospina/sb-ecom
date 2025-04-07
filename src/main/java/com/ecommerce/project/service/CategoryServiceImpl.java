package com.ecommerce.project.service;

import com.ecommerce.project.exception.EntityNotFoundException;
import com.ecommerce.project.exception.InvalidLengthException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.repositories.CategoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Override
    public List<Category> getAllCategories() {
        var categories = categoryRepository.findAll();
        if (categories.isEmpty()) {
            throw new EntityNotFoundException("No categories available.");
        }
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new InvalidLengthException("Category name cannot be empty.");
        }
        if (category.getCategoryName().length() < 5) {
            throw new InvalidLengthException("Category name must be at least 5 characters long.");
        }

        categoryRepository.save(category);
    }

    @Override
    public Category updateCategory(Category category, Long categoryId) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new InvalidLengthException("Category name cannot be empty.");
        }
        if (category.getCategoryName().trim().length() < 5) {
            throw new InvalidLengthException("Category name must be at least 5 characters long.");
        }

        return categoryRepository.findById(categoryId)
                .map(existingCategory -> {
                    existingCategory.setCategoryName(category.getCategoryName());
                    return categoryRepository.save(existingCategory);
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