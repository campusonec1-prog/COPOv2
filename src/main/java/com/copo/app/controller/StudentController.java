package com.copo.app.controller;
import com.copo.app.service.SectionService;

import com.copo.app.model.Student;
import com.copo.app.service.BatchService;
import com.copo.app.service.DepartmentService;
import com.copo.app.service.StudentService;
import jakarta.servlet.http.HttpSession;
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
public class StudentController {

    @Autowired
    private SectionService sectionService;

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private DepartmentService departmentService;
    @Autowired
    private BatchService batchService;

    @org.springframework.beans.factory.annotation.Value("${app.registration.enabled:true}")
    private boolean isRegistrationEnabled;

    // List all students
    @GetMapping("/students")
    public String getAllStudents(Model model) {
        try {
            List<Student> studentList = studentService.getAllStudents();
            model.addAttribute("studentList", studentList);
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("batches", batchService.getAllBatches());
            return "students/list";
        } catch (Exception e) {
            logger.error("Error fetching student list", e);
            model.addAttribute("error", "Unable to load student list..");
            return "error";
        }
    }

    // Student dashboard
    @GetMapping("/students/home")
    public String studentHome(HttpSession session, Model model) {
        try {
            Student student = (Student) session.getAttribute("loggedDetails");
            if (student == null) {
                return "redirect:/login";
            }
            model.addAttribute("userDetails", student);
            return "login/studentdashboard";
        } catch (Exception e) {
            logger.error("Error loading student home", e);
            model.addAttribute("error", "Unable to load dashboard..");
            return "error";
        }
    }
    
    // Filter students by department and batch
    @GetMapping(value = "/students/filter", produces = org.springframework.http.MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> filterStudents(
        @RequestParam(required = false) String department,
        @RequestParam(required = false) String batch,
        @RequestParam(required = false) String section,
        @RequestParam(required = false) Long departmentId,
        @RequestParam(required = false) Long batchId,
        Model model) {
        try {
            Map<String, Object> response = new HashMap<>();

            // Prefer IDs if provided; otherwise resolve by names
            Long resolvedDepartmentId = departmentId != null ? departmentId :
                ((department != null && !department.isEmpty()) ? departmentService.getDepartmentByName(department).getId() : null);

            Long resolvedBatchId = batchId != null ? batchId :
                ((batch != null && !batch.isEmpty()) ? batchService.getBatchByName(batch).getId() : null);

            Long sectionId = null;
            if (section != null && !section.isEmpty()) {
                sectionId = Long.valueOf(section);
            }

            List<Student> students;
            if (sectionId != null) {
                students = studentService.getStudentsByDepartmentBatchAndSectionId(resolvedDepartmentId, resolvedBatchId, sectionId);
            } else {
                students = studentService.getStudentsByDepartmentAndBatch(resolvedDepartmentId, resolvedBatchId);
            }
            response.put("students", students);
            return response;

        } catch (Exception e) {
            logger.error("Error loading filtered students", e);
            model.addAttribute("error", "Unable to load students..");
            return Map.of("error", "Unable to filter students.");
        }
    }

    // Get sections by department ID (for AJAX calls)
    @GetMapping("/students/sections")
    @ResponseBody
    public List<com.copo.app.model.Section> getSectionsByDepartment(@RequestParam Long departmentId) {
        try {
            return sectionService.getSectionsByDepartmentId(departmentId);
        } catch (Exception e) {
            logger.error("Error fetching sections for department ID: {}", departmentId, e);
            return List.of();
        }
    }

    // Show form to create student
    @GetMapping("/students/new")
    public String showCreateStudentForm(Model model) {
        try {
            model.addAttribute("student", new Student());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("batches", batchService.getAllBatches());
            model.addAttribute("sections", sectionService.getAllSections());
            return "students/create";
        } catch (Exception e) {
            logger.error("Error displaying create student form", e);
            model.addAttribute("error", "Unable to load form..");
            return "error";
        }
    }

    // Save new student
    @PostMapping("/students")
    public String createStudent(@ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        try {
            studentService.saveStudent(student);
            redirectAttributes.addFlashAttribute("success", "Student created successfully!");
        } catch (Exception e) {
            logger.error("Error creating student", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create student..");
        }
        return "redirect:/students";
    }

    // Edit student form
    @GetMapping("/students/edit/{id}")
    public String showEditStudentForm(@PathVariable Long id, Model model) {
        try {
            Student student = studentService.getStudentById(id)
                    .orElseThrow(() -> new RuntimeException("Student not found with ID: " + id));
            model.addAttribute("student", student);
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("batches", batchService.getAllBatches());
            // Only sections for the student's department
            model.addAttribute("sections", sectionService.getSectionsByDepartmentId(student.getDepartment().getId()));
            return "students/edit";
        } catch (Exception e) {
            logger.error("Error loading edit form for student ID: {}", id, e);
            model.addAttribute("error", "Unable to load edit form..");
            return "error";
        }
    }

    // Update student
    @PostMapping("/students/edit/{id}")
    public String updateStudent(@PathVariable Long id, @ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        try {
            studentService.updateStudent(id, student);
            redirectAttributes.addFlashAttribute("success", "Student updated successfully!");
        } catch (Exception e) {
            logger.error("Error updating student with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to update student..");
        }
        return "redirect:/students";
    }

    // Delete student
    @PostMapping("/students/delete/{id}")
    public String deleteStudent(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            studentService.deleteStudent(id);
            redirectAttributes.addFlashAttribute("success", "Student deleted successfully!");
        } catch (Exception e) {
            logger.error("Error deleting student with ID: {}", id, e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete student..");
        }
        return "redirect:/students";
    }

    // Public student registration - Show form
    @GetMapping("/register/student")
    public String showPublicRegistrationForm(Model model, RedirectAttributes redirectAttributes) {
        if (!isRegistrationEnabled) {
            redirectAttributes.addFlashAttribute("error", "Student self-registration is disabled in this environment. Please contact administration.");
            return "redirect:/login";
        }
        try {
            model.addAttribute("student", new Student());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("batches", batchService.getAllBatches());
            model.addAttribute("sections", sectionService.getAllSections());
            return "register/student";
        } catch (Exception e) {
            logger.error("Error displaying public registration form", e);
            model.addAttribute("error", "Unable to load registration form..");
            return "register/student";
        }
    }

    // Public student registration - Process form
    @PostMapping("/register/student")
    public String processPublicRegistration(@ModelAttribute Student student, RedirectAttributes redirectAttributes) {
        if (!isRegistrationEnabled) {
            redirectAttributes.addFlashAttribute("error", "Student self-registration is disabled in this environment.");
            return "redirect:/login";
        }
        try {
            // Validate required fields
            if (student.getName() == null || student.getName().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Name is required.");
                return "redirect:/register/student";
            }
            if (student.getRollNumber() == null || student.getRollNumber().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Roll Number is required.");
                return "redirect:/register/student";
            }
            if (student.getRegisterNumber() == null || student.getRegisterNumber().trim().isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Register Number is required.");
                return "redirect:/register/student";
            }
            if (student.getDob() == null) {
                redirectAttributes.addFlashAttribute("error", "Date of Birth is required.");
                return "redirect:/register/student";
            }
            if (student.getDepartment() == null || student.getDepartment().getId() == null) {
                redirectAttributes.addFlashAttribute("error", "Department is required.");
                return "redirect:/register/student";
            }
            if (student.getBatch() == null || student.getBatch().getId() == null) {
                redirectAttributes.addFlashAttribute("error", "Batch is required.");
                return "redirect:/register/student";
            }

            // Check if roll number already exists
            if (studentService.existsByRollNumber(student.getRollNumber())) {
                redirectAttributes.addFlashAttribute("error", "A student with this roll number already exists. Please use a different roll number.");
                return "redirect:/register/student";
            }

            // Check if register number already exists
            if (studentService.existsByRegisterNumber(student.getRegisterNumber())) {
                redirectAttributes.addFlashAttribute("error", "A student with this register number already exists. Please use a different register number.");
                return "redirect:/register/student";
            }

            // Save the student
            studentService.saveStudent(student);
            redirectAttributes.addFlashAttribute("success", "Student registered successfully! You can now login with your roll number and date of birth.");
            return "redirect:/login";
        } catch (Exception e) {
            logger.error("Error processing public student registration", e);
            redirectAttributes.addFlashAttribute("error", "Failed to register student..");
            return "redirect:/register/student";
        }
    }

    // Upload student CSV
    @PostMapping("/students/upload-csv")
    public String uploadStudentCSV(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        try {
        	logger.warn("Students upload started");
            Map<String, Object> result = studentService.uploadStudents(file);
            List<String> uploadedStudents = result.get("uploadedStudents") instanceof List<?> 
                ? ((List<?>) result.get("uploadedStudents")).stream()
                    .filter(obj -> obj instanceof String)
                    .map(obj -> (String) obj)
                    .toList()
                : List.of();
            List<String> errors = result.get("errors") instanceof List<?> 
                ? ((List<?>) result.get("errors")).stream()
                    .filter(obj -> obj instanceof String)
                    .map(obj -> (String) obj)
                    .toList()
                : List.of();

            if (!errors.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", String.join(" :: ", errors));
                logger.warn("Students upload completed with errors");
                if (!uploadedStudents.isEmpty()) {
                    redirectAttributes.addFlashAttribute("success", "Few uploaded successfully: " + uploadedStudents);
                }
            } else {
                redirectAttributes.addFlashAttribute("success", "Student data uploaded successfully!");
                logger.info("Students uploaded successfully via CSV");
            }

        } catch (Exception e) {
            logger.error("Error uploading student CSV", e);
            redirectAttributes.addFlashAttribute("error", "Error uploading student data. Please try again.");
        }
        return "redirect:/students";
    }

}



