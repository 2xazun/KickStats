package com.kickstats.controller;

import com.kickstats.dto.TeamForm;
import com.kickstats.exception.DuplicateNameException;
import com.kickstats.exception.InUseException;
import com.kickstats.service.StadiumService;
import com.kickstats.service.TeamService;
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
@RequestMapping("/teams")
@RequiredArgsConstructor
public class TeamController {

    private static final String FORM_VIEW = "teams/form";
    private static final String REDIRECT_LIST = "redirect:/teams";

    private final TeamService teamService;
    private final StadiumService stadiumService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("teams", teamService.findAll());
        return "teams/list";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("teamForm", new TeamForm());
        return formView(model);
    }

    @PostMapping
    public String create(@Valid @ModelAttribute TeamForm teamForm, BindingResult bindingResult,
            Model model, RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return formView(model);
        }
        try {
            teamService.create(teamForm);
        } catch (DuplicateNameException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return formView(model);
        }
        redirectAttributes.addFlashAttribute("message", "팀을 등록했습니다.");
        return REDIRECT_LIST;
    }

    @GetMapping("/{teamId}/edit")
    public String editForm(@PathVariable Integer teamId, Model model) {
        model.addAttribute("teamId", teamId);
        model.addAttribute("teamForm", teamService.getForm(teamId));
        return formView(model);
    }

    @PostMapping("/{teamId}")
    public String update(@PathVariable Integer teamId, @Valid @ModelAttribute TeamForm teamForm,
            BindingResult bindingResult, Model model, RedirectAttributes redirectAttributes) {
        model.addAttribute("teamId", teamId);
        if (bindingResult.hasErrors()) {
            return formView(model);
        }
        try {
            teamService.update(teamId, teamForm);
        } catch (DuplicateNameException e) {
            bindingResult.rejectValue("name", "duplicate", e.getMessage());
            return formView(model);
        }
        redirectAttributes.addFlashAttribute("message", "팀을 수정했습니다.");
        return REDIRECT_LIST;
    }

    @PostMapping("/{teamId}/delete")
    public String delete(@PathVariable Integer teamId, RedirectAttributes redirectAttributes) {
        try {
            teamService.delete(teamId);
            redirectAttributes.addFlashAttribute("message", "팀을 삭제했습니다.");
        } catch (InUseException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return REDIRECT_LIST;
    }

    private String formView(Model model) {
        model.addAttribute("stadiums", stadiumService.findAll());
        return FORM_VIEW;
    }
}
