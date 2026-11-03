package com.kickstats.controller;

import com.kickstats.dto.StadiumForm;
import com.kickstats.exception.DuplicateNameException;
import com.kickstats.service.StadiumService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/stadiums")
@RequiredArgsConstructor
public class StadiumController {

    private static final String FORM_VIEW = "stadiums/form";
    private static final String REDIRECT_LIST = "redirect:/stadiums";

    private final StadiumService stadiumService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("stadiums", stadiumService.findAll());
        return "stadiums/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("stadiumForm", new StadiumForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute StadiumForm stadiumForm, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            stadiumService.create(stadiumForm);
        } catch (DuplicateNameException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "경기장을 등록했습니다.");
        return REDIRECT_LIST;
    }

    @GetMapping("/{stadiumId}/edit")
    public String editForm(@PathVariable Integer stadiumId, Model model) {
        model.addAttribute("stadiumId", stadiumId);
        model.addAttribute("stadiumForm", stadiumService.getForm(stadiumId));
        return FORM_VIEW;
    }

    @PostMapping("/{stadiumId}")
    public String update(@PathVariable Integer stadiumId, @Valid @ModelAttribute StadiumForm stadiumForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("stadiumId", stadiumId);
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            stadiumService.update(stadiumId, stadiumForm);
        } catch (DuplicateNameException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "경기장을 수정했습니다.");
        return REDIRECT_LIST;
    }

    @PostMapping("/{stadiumId}/delete")
    public String delete(@PathVariable Integer stadiumId, RedirectAttributes redirectAttributes) {
        stadiumService.delete(stadiumId);
        redirectAttributes.addFlashAttribute("message", "경기장을 삭제했습니다.");
        return REDIRECT_LIST;
    }
}
