package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/members")
public class MembersController {

    private final UserRepository userRepository;

    public MembersController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String members(@RequestParam(value = "sort", required = false, defaultValue = "username_az") String sort,
                          Model model) {
        List<User> users = userRepository.findAll();
        // sort users according to parameter
        switch (sort) {
            case "username_za":
                users.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER).reversed());
                break;
            case "joined_asc":
                users.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
            case "joined_desc":
                users.sort(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed());
                break;
            default:
                users.sort(Comparator.comparing(User::getUsername, String.CASE_INSENSITIVE_ORDER));
        }
        List<UserSummaryDto> members = users.stream()
                .map(UserMapper::toSummaryDto)
                .collect(Collectors.toList());

        // debug log - count and first usernames
        if (members.isEmpty()) {
            System.out.println("[DEBUG] members list empty");
        } else {
            System.out.println("[DEBUG] members count=" + members.size() + ", sort=" + sort + 
                               ", first=" + members.get(0).getUsername());
        }

        model.addAttribute("members", members);
        model.addAttribute("totalCount", members.size());
        model.addAttribute("sort", sort);
        return "members";
    }
}
