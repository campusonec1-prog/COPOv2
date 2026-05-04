package com.copo.app.controller;

import com.copo.app.model.Question;
import com.copo.app.model.QuestionForm;
import com.copo.app.model.Subject;
import com.copo.app.model.Department;
import com.copo.app.service.QuestionService;
import com.copo.app.service.SubjectService;
import com.copo.app.service.BatchService;
import com.copo.app.service.DepartmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


@Controller
@RequestMapping("/questions")
public class QuestionController {

    private static final Logger logger = LoggerFactory.getLogger(QuestionController.class);

    @Autowired
    QuestionService questionService;

    @Autowired
    SubjectService subjectService;

    @Autowired
    DepartmentService departmentService;

    @Autowired
    BatchService batchService;

    @GetMapping("/filtered")
    @ResponseBody
    public List<Question> getQuestions(
            @RequestParam String department,
            @RequestParam String batch,
            @RequestParam int semester,
            @RequestParam String examType,
            @RequestParam String subject
    ) {
        logger.info("GET /questions/filtered - department={}, batch={}, semester={}, examType={}, subject={}",
                department, batch, semester, examType, subject);
        return questionService.getFilteredQuestions(department, batch, semester, examType, subject);
    }

    @GetMapping("/by-dept-sem")
    @ResponseBody
    public List<Subject> getSubjectsByDepartmentAndSemester(@RequestParam String department, @RequestParam Integer semester) {
        logger.info("GET /questions/by-dept-sem - department={}, semester={}", department, semester);
        Department departmentEntity = departmentService.getDepartmentByName(department);
        return subjectService.getSubjectsByDepartmentAndSemester(departmentEntity.getId(), semester);
    }

    @GetMapping("/new")
    public String showAddQuestionsForm(Model model) {
        logger.info("GET /questions/new");
        model.addAttribute("questionForm", new QuestionForm());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());
        return "questions/create";
    }

    @PostMapping
    public String saveQuestions(
            @RequestParam String examType,
            @RequestParam Long subjectId,
            @RequestParam Long departmentId,
            @RequestParam Long batchId,
            @RequestParam int semester,
            @ModelAttribute QuestionForm questionForm,
            RedirectAttributes redirectAttributes
    ) {
        logger.info("POST /questions - Saving questions with examType={}, subjectId={}, departmentId={}, batchId={}, semester={}",
                examType, subjectId, departmentId, batchId, semester);

        try {
            var subject = subjectService.getSubjectById(subjectId).orElseThrow(() -> new RuntimeException("Subject not found"));
            var department = departmentService.getDepartmentById(departmentId).orElseThrow(() -> new RuntimeException("Department not found"));
            var batch = batchService.getBatchById(batchId).orElseThrow(() -> new RuntimeException("Batch not found"));

            for (Question question : questionForm.getQuestions()) {
                question.setExamType(examType);
                question.setSubject(subject);
                question.setDepartment(department);
                question.setBatch(batch);
                question.setSemester(semester);
            }

            questionService.saveAllQuestions(questionForm.getQuestions());
            redirectAttributes.addFlashAttribute("success", "Questions saved successfully.");
        } catch (Exception e) {
            logger.error("Error saving questions: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to save questions. Please try again.");
        }

        return "redirect:/questions";
    }

    @GetMapping
    public String listQuestions(Model model) {
        logger.info("GET /questions");
        try {
            model.addAttribute("questions", questionService.getAllQuestions());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("batches", batchService.getAllBatches());
        } catch (Exception e) {
            logger.error("Error loading question list: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to load questions.");
        }
        return "questions/list";
    }

    @PostMapping("/delete/{id}")
    public String deleteQuestion(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        logger.info("GET /questions/delete/{}", id);
        try {
            questionService.deleteQuestion(id);
            redirectAttributes.addFlashAttribute("success", "Question deleted successfully.");
        } catch (Exception e) {
            logger.error("Error deleting question {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete question. Please try again.");
        }
        return "redirect:/questions";
    }
    
    @DeleteMapping("/delete-all-filtered")
    public ResponseEntity<String> deleteFilteredQuestions(
            @RequestParam String department,
            @RequestParam(required = false) String batch,
            @RequestParam int semester,
            @RequestParam String subject,
            @RequestParam String examType) {
        try {
        	logger.info("Deleting questions for department={}, batch={}, semester={}, subject={}, examType={}",
                    department, batch, semester, subject, examType);

            questionService.deleteQuestionsByFilters(department, batch, semester, subject, examType);

            logger.info("Successfully deleted filtered questions.");
            return ResponseEntity.ok("Deleted successfully");

        } catch (Exception e) {
        	logger.error("Error while deleting filtered questions", e);
            return ResponseEntity.internalServerError().body("Failed to delete filtered questions.");
        }
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        logger.info("GET /questions/edit/{}", id);
        try {
            Question question = questionService.getQuestionById(id);
            model.addAttribute("question", question);
            model.addAttribute("subjects", subjectService.getAllSubjects());
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("batches", batchService.getAllBatches());
        } catch (Exception e) {
            logger.error("Error showing edit form: {}", e.getMessage(), e);
            model.addAttribute("error", "Unable to load question for editing.");
        }
        return "questions/edit";
    }

    @PostMapping("/edit/{id}")
    public String updateQuestion(@PathVariable Long id, @ModelAttribute Question question, RedirectAttributes redirectAttributes) {
        logger.info("POST /questions/edit/{}", id);
        try {
            questionService.updateQuestion(id, question);
            redirectAttributes.addFlashAttribute("success", "Question updated successfully.");
        } catch (Exception e) {
            logger.error("Error updating question {}: {}", id, e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Failed to update question. Please try again.");
        }
        return "redirect:/questions";
    }

    @GetMapping("/department/{departmentId}/questions")
    public String getDepartmentQuestions(@PathVariable Long departmentId, Model model) {
        logger.info("GET /questions/department/{}/questions", departmentId);
        try {
            Department department = departmentService.getDepartmentById(departmentId)
                    .orElseThrow(() -> new RuntimeException("Department not found"));
            List<Question> questions = questionService.getQuestionsByDepartment(departmentId);
            model.addAttribute("questions", questions);
            model.addAttribute("departments", departmentService.getAllDepartments());
            model.addAttribute("selectedDepartmentName", department.getName());
        } catch (Exception e) {
            logger.error("Error fetching department questions: {}", e.getMessage(), e);
            model.addAttribute("error", "Failed to fetch department questions.");
        }
        return "questions/list";
    }

    @PostMapping("/upload-excel")
    public String uploadQuestionsCSV(@RequestParam("file") MultipartFile file, RedirectAttributes redirectAttributes) {
        logger.info("POST /questions/upload-excel - File: {}", file.getOriginalFilename());
        try {
            if (file.isEmpty() || !file.getOriginalFilename().endsWith(".xlsx")) {
                redirectAttributes.addFlashAttribute("error", "Please upload a valid non-empty .xlsx Excel file.");
                return "redirect:/questions";
            }

            questionService.saveQuestionsFromExcel(file);
            redirectAttributes.addFlashAttribute("success", "Questions uploaded successfully.");
        } catch (Exception e) {
            logger.error("Error uploading questions from Excel: {}", e.getMessage(), e);
            redirectAttributes.addFlashAttribute("error", "Upload failed. Please try again.");
        }
        return "redirect:/questions";
    }
}



