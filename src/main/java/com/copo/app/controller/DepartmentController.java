package com.copo.app.controller;

import com.copo.app.model.Department;
import com.copo.app.service.DepartmentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller
@RequestMapping("/departments")
public class DepartmentController {

    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);

    @Autowired
    private DepartmentService departmentService;

    // Get all departments
    @GetMapping
    public String getAllDepartments(Model model,@RequestParam(value = "successMessage", required = false) String successMessage,
            @RequestParam(value = "errorMessage", required = false) String errorMessage
                                    ) {
        try {
            List<Department> departments = departmentService.getAllDepartments();
            model.addAttribute("departments", departments);
            if (successMessage !=null) model.addAttribute("successMessage", successMessage);
            if (errorMessage !=null) model.addAttribute("errorMessage", errorMessage);
            return "departments/list";
        } catch (Exception e) {
            logger.error("Failed to fetch department list", e);
            model.addAttribute("errorMessage", "Failed to load department list.. Please try again.");
            return "departments/list";
        }
    }

    // Show form to create a new department
    @GetMapping("/new")
    public String showCreateDepartmentForm(Model model) {
        model.addAttribute("department", new Department());
        return "departments/create";
    }

    // Save a new department
    @PostMapping
    public String createDepartment(@ModelAttribute Department department,
                                   @RequestParam(value = "sectionNames", required = false) List<String> sectionNames,
                                   RedirectAttributes redirectAttributes) {
        try {
            Department saved = departmentService.saveDepartment(department);
            departmentService.createSectionsForDepartment(saved, sectionNames);
            logger.info("Department created: {}", department.getName());
            redirectAttributes.addFlashAttribute("successMessage", "Department added successfully.");
        } catch (Exception e) {
            logger.error("Failed to create department: {}", department.getName(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        }
        return "redirect:/departments";
    }

    // Show edit form
    @GetMapping("/edit/{id}")
    public String showEditDepartmentForm(@PathVariable Long id,
                                         Model model,
                                         RedirectAttributes redirectAttributes) {
        try {
            Department department = departmentService.getDepartmentById(id)
                .orElseThrow(() -> new RuntimeException("Department not found with id: " + id));
            model.addAttribute("department", department);
            return "departments/edit";
        } catch (Exception e) {
            logger.error("Error loading edit form for id {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please try again.");
            return "redirect:/departments";
        }
    }

    // Update a department
    @PostMapping("/edit/{id}")
    public String updateDepartment(@PathVariable Long id,
                                   @ModelAttribute Department department,
                                   @RequestParam(value = "sectionNames", required = false) List<String> sectionNames,
                                   RedirectAttributes redirectAttributes) {
        try {
            Department updated = departmentService.updateDepartment(id, department);
            // Sync sections: remove unselected, add newly selected
            departmentService.syncSectionsForDepartment(updated, sectionNames);
            logger.info("Department updated with id: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Department updated successfully.");
        } catch (Exception e) {
            logger.error("Failed to update department with id: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "An unexpected error occurred. Please try again.");
        }
        return "redirect:/departments";
    }

    // Delete a department
    @PostMapping("/delete/{id}")
    public String deleteDepartment(@PathVariable Long id,
                                   RedirectAttributes redirectAttributes) {
        try {
            departmentService.deleteDepartment(id);
            logger.info("Department deleted with id: {}", id);
            redirectAttributes.addFlashAttribute("successMessage", "Department deleted successfully.");
        } catch (Exception e) {
            logger.error("Failed to delete department with id: {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to delete department.");
        }
        return "redirect:/departments";
    }

    // Get department ID by name (for AJAX calls)
    @GetMapping("/id")
    @ResponseBody
    public Department getDepartmentByName(@RequestParam String name) {
        try {
            return departmentService.getDepartmentByName(name);
        } catch (Exception e) {
            logger.error("Error fetching department by name: {}", name, e);
            return null;
        }
    }
    
}



