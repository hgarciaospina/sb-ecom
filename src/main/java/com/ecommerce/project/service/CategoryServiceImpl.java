package com.ecommerce.project.service;

import com.ecommerce.project.exception.CategoryNotFoundException;
import com.ecommerce.project.exception.InvalidCategoryException;
import com.ecommerce.project.model.Category;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {
    private static Long nextId = 1L;
    private final List<Category> categories = new ArrayList<>();

    @Override
    public List<Category> getAllCategories() {
        if (categories.isEmpty()) {
            throw new CategoryNotFoundException("No categories available.");
        }
        return categories;
    }

    @Override
    public void createCategory(Category category) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new InvalidCategoryException("Category name cannot be empty.");
        }
        category.setCategoryId(nextId++);
        categories.add(category);
    }

    @Override
    public Category updateCategory(Category category, Long categoryId) {
        if (category.getCategoryName() == null || category.getCategoryName().trim().isEmpty()) {
            throw new InvalidCategoryException("Category name cannot be empty.");
        }

        return categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst()
                .map(existingCategory -> {
                    existingCategory.setCategoryName(category.getCategoryName());
                    return existingCategory;
                })
                .orElseThrow(() -> new CategoryNotFoundException("Category with ID " + categoryId + " not found"));
    }

    @Override
    public void deleteCategory(Long categoryId) {
        categories.stream()
                .filter(c -> c.getCategoryId().equals(categoryId))
                .findFirst()
                .ifPresentOrElse(categories::remove,
                        () -> { throw new CategoryNotFoundException("Category with categoryId: " + categoryId + " not found."); });
    }

}
