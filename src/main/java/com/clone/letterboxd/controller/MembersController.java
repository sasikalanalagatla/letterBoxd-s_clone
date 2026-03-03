package com.clone.letterboxd.controller;

import com.clone.letterboxd.dto.UserSummaryDto;
import com.clone.letterboxd.mapper.UserMapper;
import com.clone.letterboxd.model.User;
import com.clone.letterboxd.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
    public String members(Model model) {
        List<User> users = userRepository.findAll();
        List<UserSummaryDto> members = users.stream()
                .map(UserMapper::toSummaryDto)
                .collect(Collectors.toList());

        model.addAttribute("members", members);
        model.addAttribute("totalCount", members.size());
        return "members";
    }
}
