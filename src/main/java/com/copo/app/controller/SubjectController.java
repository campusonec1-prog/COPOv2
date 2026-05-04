package com.copo.app.controller;

import com.copo.app.model.Department;
import com.copo.app.model.Subject;
import com.copo.app.service.DepartmentService;
import com.copo.app.service.SubjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/subjects")
public class SubjectController {

    private static final Logger logger = LoggerFactory.getLogger(SubjectController.class);

    @Autowired
    SubjectService subjectService;

    @Autowired
    DepartmentService departmentService;

    // ✅ 1. Filtered subjects (AJAX call)
    @GetMapping("/filtered")
    @ResponseBody
    public Map<String, Object> filterSubjects(@RequestParam(required = false) Long departmentId,
                                              @RequestParam(required = false) Integer semester) {
        Map<String, Object> response = new HashMap<>();
        try {
            List<Subject> subjects = subjectService.getFilteredSubjects(departmentId, semester);
            response.put("subjects", subjects);
            logger.info("Filtered subjects fetched for deptId: {}, semester: {}", departmentId, semester);
        } catch (Exception e) {
            logger.error("Failed to fetch filtered subjects", e);
            response.put("error", "Failed to fetch subjects.");
        }
        return response;
    }

    // ✅ 2. Subjects by deptId and semester (raw Long ID)
    @GetMapping("/by-deptId-sem")
    @ResponseBody
    public List<Subject> getSubjectsByDepartmentIDAndSemester(@RequestParam Long department,
                                                              @RequestParam Integer semester) {
        try {
            List<Subject> subjects = subjectService.getSubjectsByDepartmentAndSemester(department, semester);
            logger.info("Fetched subjects for deptId={}, semester={}", department, semester);
            return subjects;
        } catch (Exception e) {
            logger.error("Error fetching subjects by deptId and semester", e);
            return List.of();
        }
    }

    // ✅ 3. Subjects by department name and semester
    @GetMapping("/by-dept-sem")
    @ResponseBody
    public List<Subject> getSubjectsByDepartmentAndSemester(@RequestParam String department,
                                                            @RequestParam Integer semester) {
        try {
            Department dept = departmentService.getDepartmentByName(department);
            List<Subject> subjects = subjectService.getSubjectsByDepartmentAndSemester(dept.getId(), semester);
            logger.info("Fetched subjects for department={}, semester={}", department, semester);
            return subjects;
        } catch (Exception e) {
            logger.error("Error fetching subjects by department name", e);
            return List.of();
        }
    }

    // ✅ 4. Get all subjects
    @GetMapping
    public String getAllSubjects(Model model) {
        try {
            List<Subject> subjects = subjectService.getAllSubjects();
            List<Department> departments = departmentService.getAllDepartments();
            model.addAttribute("departments", departments);
            model.addAttribute("subjects", subjects);
            logger.info("All subjects listed successfully");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to fetch subjects.");
            logger.error("Error listing all subjects", e);
        }
        return "subjects/list";
    }

    // ✅ 5. Show create form
    @GetMapping("/new")
    public String showCreateSubjectForm(Model model) {
        try {
            model.addAttribute("subject", new Subject());
            model.addAttribute("departments", departmentService.getAllDepartments());
            logger.info("Create subject form displayed");
        } catch (Exception e) {
            model.addAttribute("error", "Failed to load create subject form.");
            logger.error("Error loading create subject form", e);
        }
        return "subjects/create";
    }

    // ✅ 6. Create subject
    @PostMapping
    public String createSubject(@ModelAttribute Subject subject, RedirectAttributes redirectAttributes) {
        try {
            subjectService.saveSubject(subject);
            redirectAttributes.addFlashAttribute("success", "Subject created successfully.");
            logger.info("Subject created: {}", subject.getName());
        } catch (Exception e) {
            logger.error("Error creating subject: {}", subject.getName(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to create subject. Please try again.");
        }
        return "redirect:/subjects";
    }

    // ✅ 7. Show edit form
    @GetMapping("/edit/{id}")
    public String showEditSubjectForm(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Subject subject = subjectService.getSubjectById(id)
                    .orElseThrow(() -> new RuntimeException("Subject not found!"));
            model.addAttribute("subject", subject);
            model.addAttribute("departments", departmentService.getAllDepartments());
            logger.info("Edit form loaded for subject id={}", id);
            return "subjects/edit";
        } catch (Exception e) {
            logger.error("Error loading edit form for subject id={}", id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to load edit form. Please try again.");
            return "redirect:/subjects";
        }
    }

    // ✅ 8. Update subject
    @PostMapping("/edit/{id}")
    public String updateSubject(@PathVariable Long id, @ModelAttribute Subject subject, RedirectAttributes redirectAttributes) {
        try {
            subjectService.updateSubject(id, subject);
            redirectAttributes.addFlashAttribute("success", "Subject updated successfully.");
            logger.info("Subject updated: {}", subject.getName());
        } catch (Exception e) {
            logger.error("Error updating subject with id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update subject. Please try again.");
        }
        return "redirect:/subjects";
    }

    // ✅ 9. Delete subject
    @PostMapping("/delete/{id}")
    public String deleteSubject(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            subjectService.deleteSubject(id);
            redirectAttributes.addFlashAttribute("success", "Subject deleted successfully.");
            logger.info("Subject deleted with id: {}", id);
        } catch (Exception e) {
            logger.error("Error deleting subject with id {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete subject. Please try again.");
        }
        return "redirect:/subjects";
    }

    // ✅ 10. Upload subjects from CSV
    @PostMapping("/upload")
    public String uploadSubjects(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
        	Map<String, Object> result = subjectService.uploadSubjects(file);
            List<String> uploadedSubjects = result.get("uploadedSubjects") instanceof List
                    ? ((List<?>) result.get("uploadedSubjects")).stream()
                        .filter(obj -> obj instanceof String)
                        .map(obj -> (String) obj)
                        .toList()
                    : List.of();
            List<String> errors = result.get("errors") instanceof List
                    ? ((List<?>) result.get("errors")).stream()
                        .filter(obj -> obj instanceof String)
                        .map(obj -> (String) obj)
                        .toList()
                    : List.of();
            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", String.join("<br/>", errors));
                logger.warn("Subject upload completed with errors");
                if(!uploadedSubjects.isEmpty()) {
                	redirectAttributes.addFlashAttribute("success", "Few Uploaded successfully"+uploadedSubjects);
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "CSV uploaded successfully without any errors.");
                logger.info("Subjects uploaded successfully via CSV");
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Unexpected error. Please try again.");
            logger.error("Error during CSV upload", e);
        }
        return "redirect:/subjects";
    }
}



