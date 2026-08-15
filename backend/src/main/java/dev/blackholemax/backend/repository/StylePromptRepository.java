package dev.blackholemax.backend.repository;

import dev.blackholemax.backend.entity.StylePrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface StylePromptRepository extends JpaRepository<StylePrompt, Long> {

    Optional<StylePrompt> findByCode(String code);

    @Override
    List<StylePrompt> findAll();
}
