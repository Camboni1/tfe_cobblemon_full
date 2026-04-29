package be.loic.tfe_cobblemon.item.controller;

import be.loic.tfe_cobblemon.item.dto.ItemResponse;
import be.loic.tfe_cobblemon.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/items")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000")
public class ItemController {

    private final ItemService itemService;

    @GetMapping
    public Page<ItemResponse> search(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return itemService.search(search, pageable);
    }
}