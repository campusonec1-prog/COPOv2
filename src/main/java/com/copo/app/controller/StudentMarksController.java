package com.copo.app.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.copo.app.model.Department;
import com.copo.app.model.Question;
import com.copo.app.model.QuestionForm;
import com.copo.app.model.Student;
import com.copo.app.model.StudentMarks;
import com.copo.app.model.Subject;
import com.copo.app.repository.StudentMarksProjection;
import com.copo.app.service.BatchService;
import com.copo.app.service.DepartmentService;
import com.copo.app.service.QuestionService;
import com.copo.app.service.StudentMarksService;
import com.copo.app.service.StudentService;
import com.copo.app.service.SubjectService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/student-marks")
public class StudentMarksController {

    @Autowired
    private StudentMarksService studentMarksService;
    @Autowired
    QuestionService questionService;
	@Autowired
    SubjectService subjectService;
	@Autowired
    DepartmentService departmentService;
	@Autowired
    BatchService batchService;;
    @Autowired
    StudentService studentService;

    @GetMapping
    public String getStudentMarks(Model model,HttpSession session) {
        Student student = (Student) session.getAttribute("loggedDetails");
        if (student == null) {
        	return "redirect:/login";
        }
        System.out.println("start 1");
        model.addAttribute("questionForm", new QuestionForm());
        model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());
        model.addAttribute("studentMarks", studentMarksService.getMarksByStudent(student));
        model.addAttribute("student", studentService.getStudentById(student.getId()));
        return "studentmarks/student_marks";
    }
    
    // ✅ API to Fetch Student Details for AJAX
    @GetMapping("/details")
    @ResponseBody
    public Map<String, Object> getStudentDetails(HttpSession session) {
        Student student = (Student) session.getAttribute("loggedDetails");
        if (student == null) {
            throw new RuntimeException("User not logged in");
        }

        Map<String, Object> response = new HashMap<>();
        response.put("department", student.getDepartment());
        response.put("batch", student.getBatch());
        response.put("studentId", student.getId()); // Add this line to include studentId
        
        System.out.println("student ID =" + student.getId()+"  - "+ student.getName());

        return response;
    }

    @PostMapping("/submit")
    @ResponseBody
    public String submitStudentMarks(@RequestBody List<StudentMarks> marks, HttpSession session) {
        Student student = (Student) session.getAttribute("loggedDetails");
        if (student == null) {
            throw new RuntimeException("User not logged in");
        }

        System.out.println("Submitting marks for Student ID: " + student.getId());
        
        System.out.println("Final Marks to Save: " + marks);

        marks.forEach(mark -> {
            mark.setStudent(student);

            // Convert the incoming Question ID to a proper Question entity
            if (mark.getQuestion() != null && mark.getQuestion().getId() != null) {
                Question fullQuestion = questionService.getQuestionById(mark.getQuestion().getId());
                mark.setQuestion(fullQuestion);
            } else {
                throw new RuntimeException("Question ID is missing in submitted marks.");
            }
        });

        System.out.println("Final Marks to Save: " + marks);
        studentMarksService.saveStudentMarks(marks);
        return "Marks submitted successfully!";
    }


    
    
 // ✅ Endpoint to fetch questions and submitted marks
    @GetMapping("/filtered")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFilteredData(
        @RequestParam Long studentId,
        @RequestParam String department,
        @RequestParam String batch,
        @RequestParam int semester,
        @RequestParam String examType,
        @RequestParam String subject) {

        System.out.println("Fetching data for Student ID: " + studentId + ", Department: " + department + 
            ", Batch: " + batch + ", Semester: " + semester + ", Exam Type: " + examType + ", Subject: " + subject);

        // Fetch all questions based on criteria
        List<Question> questions = questionService.getFilteredQuestions(department, batch, semester, examType, subject);
        System.out.println("Fetched Questions: " + questions);

        Department dept = departmentService.getDepartmentByName(department);
        System.out.println("Fetched Department: " + dept);

        Long departmentId = dept.getId();
        System.out.println("Department ID: " + departmentId);

        Long batchId = batchService.getBatchByName(batch).getId();
        System.out.println("Batch ID: " + batchId);

        Long subjectId = subjectService.getSubjectByDepartmentSemesterAndName(dept, semester, subject).getId();
        System.out.println("Subject ID: " + subjectId);

        // Check if the student has already submitted marks
        List<StudentMarksProjection> submittedMarks = studentMarksService.getMarksByStudentIdAndCriteria(
            studentId, departmentId, batchId, semester, examType, subjectId);
        System.out.println("Submitted Marks: " + submittedMarks);

        // Create a combined response
        Map<String, Object> response = new HashMap<>();
        response.put("questions", questions); // Add questions
        response.put("submittedMarks", submittedMarks); // Add submitted marks if they exist

        System.out.println("Response ==> " + response);

        return ResponseEntity.ok(response);
    }
    
}



