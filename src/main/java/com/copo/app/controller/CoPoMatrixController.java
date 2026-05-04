package com.copo.app.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.copo.app.model.CoPoMatrixEntry;
import com.copo.app.service.CoPoMatrixService;
import com.copo.app.service.DepartmentService;
import com.copo.app.service.SubjectService;

import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/copo")
public class CoPoMatrixController {

    @Autowired
    private CoPoMatrixService service;

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private SubjectService subjectService;

    // Load CO-PO Matrix form
    @GetMapping("/form")
    public String showCopoMatrixForm(Model model) {
    	System.out.println("copo form started");
    	model.addAttribute("outcomes", List.of(
                "PO1", "PO2", "PO3", "PO4", "PO5", "PO6",
                "PO7", "PO8", "PO9", "PO10", "PO11", "PO12",
                "PSO1", "PSO2","PSO3"));
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("subjects", subjectService.getAllSubjects()); // 👈 Load subjects
        return "copomatrix/list"; // Path to copo.html or form.html
    }

    @PostMapping("/save")
    public String saveMatrix(@RequestParam String subjectCode,
                              @RequestParam String subjectName,
                              @RequestParam Map<String, String> allParams) {
        service.saveMatrix(subjectCode, subjectName, allParams);
        return "redirect:/copo/form";
    }

    @GetMapping("/list")
    public String listMatrix(@RequestParam String subjectCode, Model model) {
        List<CoPoMatrixEntry> entries = service.getMatrixBySubjectCode(subjectCode);
        model.addAttribute("entries", entries);
        return "copomatrix/list";
    }
    
    @GetMapping("/matrix")
    public ResponseEntity<?> getMatrixData(
        @RequestParam String subjectName,
        @RequestParam String subjectCode
    ) {
        try {
        	System.out.println("Starting of matrix");
            Map<String, Object> matrixData = service.getMatrixData(subjectName, subjectCode);
            System.out.println("Mapped data  ::  "+matrixData);
            return ResponseEntity.ok(matrixData);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @PostMapping("/upload-excel")
    public String uploadCopoExcel(@RequestParam("file") MultipartFile file,
                                  @RequestParam("subjectCode") String subjectCode,
                                  @RequestParam("subjectName") String subjectName,
                                  RedirectAttributes redirectAttributes) {
        try {
        	service.parseAndSaveExcel(file, subjectCode.trim(), subjectName.trim());
            redirectAttributes.addFlashAttribute("success", "CO-PO matrix uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Upload failed. Please try again.");
        }
        return "redirect:/copo/form"; // Adjust this redirect as needed
    }


}


