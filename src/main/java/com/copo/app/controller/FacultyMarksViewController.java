package com.copo.app.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.copo.app.service.BatchService;
import com.copo.app.service.DepartmentService;
import com.copo.app.service.FacultyMarksViewService;
import com.copo.app.service.QuestionService;
import com.copo.app.service.SubjectService;
import com.copo.app.model.Subject;

@Controller
@RequestMapping("/faculty-marks")
public class FacultyMarksViewController {

    @Autowired
    private FacultyMarksViewService facultyMarksViewService;

    @Autowired
    QuestionService questionService;
	@Autowired
    SubjectService subjectService;
	@Autowired
    DepartmentService departmentService;
	@Autowired
    BatchService batchService;

    // Render the Faculty Marks View Page
    @GetMapping
    public String renderFacultyMarksPage(Model model) {
        // Fetch available departments, batches, and subjects for dropdowns
    	model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());

        // Return Thymeleaf view (HTML file)
        return "studentmarks/facultyMarksView"; // This will render `facultyMarksView.html`
    }
    
    @GetMapping("/by-dept-sem")
    @ResponseBody
    public List<Subject> getSubjectsByDepartmentAndSemester(@RequestParam Long department, @RequestParam Integer semester) {
    	
        return subjectService.getSubjectsByDepartmentAndSemester(department, semester);
    }


    // Fetch Filtered Marks (For AJAX Requests)
    @GetMapping("/view")
    @ResponseBody
    public Map<String, Object> getFilteredMarks(
            @RequestParam Long departmentId,
            @RequestParam Long batchId,
            @RequestParam Integer semester,
            @RequestParam String examType,
            @RequestParam Long subjectId,
            @RequestParam(required = false) Integer sectionId) {
    	
    	//System.out.println("Inputs = "+departmentId+" - "+batchId+" - "+semester+" - "+examType+" - "+subjectId);
    	//System.out.println("View Result : "+facultyMarksViewService.getFilteredMarks(departmentId, batchId, semester, examType, subjectId));
        return facultyMarksViewService.getFilteredMarks(departmentId, batchId, semester, examType, subjectId, sectionId);
        
        
    }
    
    
    
    @GetMapping("/fullView")
    public String renderFacultyMarksFullPage(Model model) {
        // Fetch available departments, batches, and subjects for dropdowns
    	model.addAttribute("subjects", subjectService.getAllSubjects());
        model.addAttribute("departments", departmentService.getAllDepartments());
        model.addAttribute("batches", batchService.getAllBatches());

        // Return Thymeleaf view (HTML file)
        return "studentmarks/facultyMarksFullView"; // This will render `facultyMarksView.html`
    }
    
    
    @GetMapping("/view-marks-full")
    @ResponseBody
    public Map<String, Object> viewAllMarksGrouped(
        @RequestParam Long departmentId,
        @RequestParam Long batchId,
        @RequestParam Integer semester,
        @RequestParam Long subjectId,
        Model model
    ) {

    	//System.out.println("===========================================================================================================================");
    	//System.out.println("departmentId "+departmentId+" batchId "+batchId+" semester "+semester+" subjectId "+subjectId);
    	//System.out.println("===========================================================================================================================");
    	//System.out.println("View Full Marks : --->>>  "+ facultyMarksViewService.getFullGroupedMarksWithoutExamType(departmentId, batchId, semester, subjectId));
    	//System.out.println("===========================================================================================================================");
        return facultyMarksViewService.getFullGroupedMarksWithoutExamType(departmentId, batchId, semester, subjectId); 
    }

    
    
    
    
    
    
    
}



