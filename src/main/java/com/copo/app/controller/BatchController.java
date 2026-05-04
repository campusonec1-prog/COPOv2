package com.copo.app.controller;

import com.copo.app.exception.BatchNotFoundException;
import com.copo.app.model.Batch;
import com.copo.app.service.BatchService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/batches")
public class BatchController {

    private static final Logger logger = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private BatchService batchService;

    // Get all batches
    @GetMapping
    public String getAllBatches(Model model,
                                @RequestParam(value = "success", required = false) String successMsg,
                                @RequestParam(value = "error", required = false) String errorMsg) {
        try {
            List<Batch> batches = batchService.getAllBatches();
            model.addAttribute("batches", batches);
            if (successMsg != null) model.addAttribute("success", successMsg);
            if (errorMsg != null) model.addAttribute("error", errorMsg);
            return "batches/list";
        } catch (Exception e) {
            logger.error("Error loading batch list", e);
            model.addAttribute("error", "Error loading batch list.");
            return "batches/list";
        }
    }

    // Show form for creating a new batch
    @GetMapping("/new")
    public String showCreateBatchForm(Model model) {
        model.addAttribute("batch", new Batch());
        return "batches/create";
    }

    // Save a new batch
    @PostMapping
    public String createBatch(@ModelAttribute Batch batch, RedirectAttributes redirectAttributes, Model model) {
        try {
            if (batchService.isBatchNameExists(batch.getName())) {
                model.addAttribute("error", "Batch name already exists!");
                model.addAttribute("batch", batch);
                return "batches/create";
            }

            batchService.saveBatch(batch);
            redirectAttributes.addFlashAttribute("success", "Batch added successfully.");
            logger.info("New batch '{}' created.", batch.getName());
            return "redirect:/batches";
        } catch (Exception e) {
            logger.error("Error creating batch: {}", e.getMessage());
            model.addAttribute("error", "An error occurred while saving the batch.");
            return "batches/create";
        }
    }

    // Show form for editing a batch
    @GetMapping("/edit/{id}")
    public String showEditBatchForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Batch batch = batchService.getBatchById(id)
                    .orElseThrow(() -> new BatchNotFoundException("Batch not found with ID: " + id));
            model.addAttribute("batch", batch);
            return "batches/edit";
        } catch (BatchNotFoundException e) {
            logger.warn(e.getMessage());
            redirectAttributes.addAttribute("error", "An unexpected error occurred. Please try again.");
            return "redirect:/batches";
        } catch (Exception e) {
            logger.error("Error loading edit form for batch ID: {}", id, e);
            redirectAttributes.addAttribute("error", "Error loading batch edit form.");
            return "redirect:/batches";
        }
    }

    // Update a batch
    @PostMapping("/edit/{id}")
    public String updateBatch(@PathVariable Long id, @ModelAttribute Batch batch, RedirectAttributes redirectAttributes, Model model) {
        try {
            // Optional: check if name is changed and already exists
            Batch existingBatch = batchService.getBatchById(id).orElseThrow(() -> new RuntimeException("Error while checking Batch ID existing : "+id));
            if (!existingBatch.getName().equals(batch.getName()) && batchService.isBatchNameExists(batch.getName())) {
                model.addAttribute("error", "Another batch with this name already exists!");
                model.addAttribute("batch", batch);
                return "batches/edit";
            }

            batchService.updateBatch(id, batch);
            redirectAttributes.addFlashAttribute("success", "Batch updated successfully.");
            logger.info("Batch ID {} updated.", id);
            return "redirect:/batches";
        } catch (BatchNotFoundException e) {
            logger.error("Batch not found: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "An unexpected error occurred. Please try again.");
            return "redirect:/batches";
        } catch (Exception e) {
            logger.error("Error updating batch: {}", e.getMessage());
            model.addAttribute("error", "An error occurred while updating the batch.");
            return "batches/edit";
        }
    }

    // Delete a batch
    @PostMapping("/delete/{id}")
    public String deleteBatch(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            batchService.deleteBatch(id);
            redirectAttributes.addAttribute("success", "Batch deleted successfully.");
            logger.info("Batch deleted: ID={}", id);
        } catch (BatchNotFoundException e) {
            logger.warn(e.getMessage());
            redirectAttributes.addAttribute("error", "An unexpected error occurred. Please try again.");
        } catch (Exception e) {
            logger.error("Error deleting batch ID: {}", id, e);
            redirectAttributes.addAttribute("error", "Failed to delete batch.");
        }
        return "redirect:/batches";
    }
}



