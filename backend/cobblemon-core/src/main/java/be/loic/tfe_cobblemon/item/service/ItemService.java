package be.loic.tfe_cobblemon.item.service;

import be.loic.tfe_cobblemon.item.dto.ItemResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ItemService {
    Page<ItemResponse> search(String search, Pageable pageable);
}