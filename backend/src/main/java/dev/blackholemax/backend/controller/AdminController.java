package dev.blackholemax.backend.controller;

import dev.blackholemax.backend.dto.PortfolioFormDTO;
import dev.blackholemax.backend.entity.Portfolio;
import dev.blackholemax.backend.repository.PortfolioRepository;
import dev.blackholemax.backend.repository.VisitRecordRepository;
import dev.blackholemax.backend.service.AuthService;
import dev.blackholemax.backend.service.FileStorage;
import dev.blackholemax.backend.service.PortfolioService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class AdminController {

    private static final List<String> TEMPLATES = List.of("card", "gallery", "magazine", "custom");

    private final PortfolioService portfolioService;
    private final FileStorage fileStorage;
    private final AuthService authService;
    private final PortfolioRepository portfolioRepository;
    private final VisitRecordRepository visitRecordRepository;

    public AdminController(PortfolioService portfolioService,
                           FileStorage fileStorage,
                           AuthService authService,
                           PortfolioRepository portfolioRepository,
                           VisitRecordRepository visitRecordRepository) {
        this.portfolioService = portfolioService;
        this.fileStorage = fileStorage;
        this.authService = authService;
        this.portfolioRepository = portfolioRepository;
        this.visitRecordRepository = visitRecordRepository;
    }

    @GetMapping("/admin/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/admin/register")
    public String registerPage() {
        return "register";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        long userId = authService.currentUserId();
        List<Portfolio> portfolios = portfolioRepository.findByUserIdOrderByIdDesc(userId);
        Map<Long, Long> pvMap = new HashMap<>();
        Map<Long, Long> uvMap = new HashMap<>();
        for (Portfolio p : portfolios) {
            pvMap.put(p.getId(), visitRecordRepository.countByPortfolioId(p.getId()));
            uvMap.put(p.getId(), visitRecordRepository.countDistinctVisitorByPortfolioId(p.getId()));
        }
        model.addAttribute("portfolios", portfolios);
        model.addAttribute("pvMap", pvMap);
        model.addAttribute("uvMap", uvMap);
        return "dashboard";
    }

    @GetMapping("/admin/editor")
    public String editor(@RequestParam(required = false) String slug, Model model) {
        long userId = authService.currentUserId();
        Portfolio portfolio = null;
        if (slug != null && !slug.isBlank()) {
            portfolio = portfolioRepository.findBySlug(slug)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "作品集不存在"));
            if (!portfolio.getUserId().equals(userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "无权编辑他人的作品集");
            }
        }
        model.addAttribute("portfolio", portfolio);
        model.addAttribute("projects", portfolio != null ? portfolio.getProjects() : List.of());
        model.addAttribute("templates", TEMPLATES);
        return "editor";
    }

    @PostMapping("/admin/publish")
    public String publish(
            @RequestParam String slug,
            @RequestParam String userName,
            @RequestParam(required = false) String slogan,
            @RequestParam(required = false) String bio,
            @RequestParam(required = false) String skills,
            @RequestParam(required = false) String themeColor,
            @RequestParam(required = false) String template,
            @RequestParam(name = "isPublished", required = false, defaultValue = "false") boolean isPublished,
            @RequestParam(required = false) String seoTitle,
            @RequestParam(required = false) String seoDescription,
            @RequestParam(required = false) String generatedHtml,
            @RequestParam(required = false) String customPrompt,
            @RequestParam(required = false) MultipartFile avatar,
            @RequestParam(required = false) List<String> title,
            @RequestParam(required = false) List<String> description,
            @RequestParam(required = false) List<MultipartFile> cover
    ) {
        List<String> projectTitles = title == null ? List.of() : title;
        List<String> projectDescriptions = description == null ? List.of() : description;

        List<String> projectCovers = new ArrayList<>();
        if (cover != null) {
            for (MultipartFile file : cover) {
                projectCovers.add(fileStorage.save(file));
            }
        }

        PortfolioFormDTO dto = new PortfolioFormDTO(
                slug.trim(),
                userName,
                slogan,
                bio,
                skills,
                themeColor,
                template,
                isPublished,
                fileStorage.save(avatar),
                seoTitle,
                seoDescription,
                generatedHtml,
                customPrompt,
                projectTitles,
                projectDescriptions,
                projectCovers
        );

        Portfolio saved = portfolioService.save(dto, authService.currentUserId());
        return "redirect:/p/" + saved.getSlug();
    }
}