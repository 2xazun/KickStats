package com.kickstats.controller;

import com.kickstats.dto.LeagueForm;
import com.kickstats.exception.DuplicateNameException;
import com.kickstats.service.LeagueService;
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
@RequestMapping("/leagues")
@RequiredArgsConstructor
public class LeagueController {

    private static final String FORM_VIEW = "leagues/form";
    private static final String REDIRECT_LIST = "redirect:/leagues";

    private final LeagueService leagueService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("leagues", leagueService.findAll());
        return "leagues/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("leagueForm", new LeagueForm());
        return FORM_VIEW;
    }

    @PostMapping
    public String create(@Valid @ModelAttribute LeagueForm leagueForm, BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            leagueService.create(leagueForm);
        } catch (DuplicateNameException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "리그를 등록했습니다.");
        return REDIRECT_LIST;
    }

    @GetMapping("/{leagueId}/edit")
    public String editForm(@PathVariable Integer leagueId, Model model) {
        model.addAttribute("leagueId", leagueId);
        model.addAttribute("leagueForm", leagueService.getForm(leagueId));
        return FORM_VIEW;
    }

    @PostMapping("/{leagueId}")
    public String update(@PathVariable Integer leagueId, @Valid @ModelAttribute LeagueForm leagueForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("leagueId", leagueId);
        if (bindingResult.hasErrors()) {
            return FORM_VIEW;
        }
        try {
            leagueService.update(leagueId, leagueForm);
        } catch (DuplicateNameException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return FORM_VIEW;
        }
        redirectAttributes.addFlashAttribute("message", "리그를 수정했습니다.");
        return REDIRECT_LIST;
    }

    @PostMapping("/{leagueId}/delete")
    public String delete(@PathVariable Integer leagueId, RedirectAttributes redirectAttributes) {
        leagueService.delete(leagueId);
        redirectAttributes.addFlashAttribute("message", "리그를 삭제했습니다.");
        return REDIRECT_LIST;
    }
}
