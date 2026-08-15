package dev.blackholemax.backend.controller;

import dev.blackholemax.backend.service.PortfolioQueryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class ViewerController {

    private final PortfolioQueryService portfolioQueryService;

    public ViewerController(PortfolioQueryService portfolioQueryService) {
        this.portfolioQueryService = portfolioQueryService;
    }

    @GetMapping("/p/{slug}")
    public String view(@PathVariable String slug, Model model) {
        var portfolio = portfolioQueryService.findPublishedBySlug(slug);
        if (portfolio == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "作品集不存在或未发布");
        }

        model.addAttribute("portfolio", portfolio);
        model.addAttribute("projects", portfolio.projects());

        String template = portfolio.template();
        if (template == null || template.isBlank()) {
            template = "card";
        }
        return "portfolio/" + template;
    }
}