package com.example.sweater.controller;

import com.example.sweater.domain.Message;
import com.example.sweater.domain.User;
import com.example.sweater.domain.dto.MessageDto;
import com.example.sweater.repos.MessageRepo;
import com.example.sweater.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

import javax.validation.Valid;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Controller
public class MessageController {
    private final MessageRepo messageRepo;
    private final MessageService messageService;

    @Value("${upload.path}")
    private String uploadPath;

    @Autowired
    public MessageController(MessageRepo messageRepo, MessageService messageService) {
        this.messageRepo = messageRepo;
        this.messageService = messageService;
    }

    @GetMapping("/")
    public String greeting(Map<String, Object> model) {
        return "greeting";
    }

    @GetMapping("/main")
    public String main(
            @RequestParam(required = false, defaultValue = "") String filter,
            Model model,
            @PageableDefault(sort = { "id" }, direction = Sort.Direction.DESC) Pageable pageable,
            @AuthenticationPrincipal User user
    ) {
        Page<MessageDto> page = messageService.messageList(pageable, filter, user);

        model.addAttribute("page", page);
        model.addAttribute("url", "/main");
        model.addAttribute("filter", filter);

        return "main";
    }

    @PostMapping("/main")
    public String add(@AuthenticationPrincipal User user,
                      @Valid Message message,
                      BindingResult bindingResult,
                      Model model,
                      @RequestParam("file") MultipartFile file,
                      @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable)
    {
        message.setAuthor(user);

        Page<MessageDto> pages = messageService.messageListForUser(pageable, message.getAuthor(), user);

        if (bindingResult.hasErrors()) {
            Map<String, String> errorMap = ControllerUtil.getErrors(bindingResult);
            model.mergeAttributes(errorMap);
            model.addAttribute("message", message);
        } else {
            saveFile(message, file);

            model.addAttribute("message", null);

            messageRepo.save(message);
        }
        Iterable<Message> messages = messageRepo.findAll();
        model.addAttribute("messages", messages);
        model.addAttribute("url", "/main");
        model.addAttribute("page", pages);

        return "main";
    }

    private void saveFile(@Valid Message message, @RequestParam("file") MultipartFile file) {
        if (file != null && !file.getOriginalFilename().isEmpty()) {
            File fileDir = new File(uploadPath);
            if (!fileDir.exists())
                fileDir.mkdir();

            String uuidFile = UUID.randomUUID().toString();
            String resultFilename = uuidFile + "." + file.getOriginalFilename();

            try {
                file.transferTo(new File(fileDir + "/" + resultFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }

            message.setFilename(resultFilename);
        }
    }

    @GetMapping("/user-messages/{author}")
    public String userMessages(
            @AuthenticationPrincipal User currentUser,
            @PathVariable User author,
            Model model,
            @RequestParam(required = false) Message message,
            @PageableDefault(sort = {"id"}, direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<MessageDto> page = messageService.messageListForUser(pageable, author, currentUser);

        model.addAttribute("page", page);
        model.addAttribute("message", message);
        model.addAttribute("isCurrentUser", currentUser.equals(author));
        model.addAttribute("userChannel", author);
        model.addAttribute("subscribersCount", author.getSubscribers().size());
        model.addAttribute("subscriptionsCount", author.getSubscriptions().size());
        model.addAttribute("isSubscriber", author.getSubscribers().contains(currentUser));
        model.addAttribute("url", "/user-message/" + author.getId());

        return "userMessages";
    }

    @PostMapping("/user-messages/{user}")
    public String updateMessage(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Long user,
            @RequestParam("id") Message message,
            @RequestParam("text") String text,
            @RequestParam("tag") String tag,
            @RequestParam("file") MultipartFile file
    ) {
        if (message.getAuthor().equals(currentUser)) {
            if (!StringUtils.isEmpty(text))
                message.setText(text);

            if (!StringUtils.isEmpty(tag))
                message.setTag(tag);

            saveFile(message, file);

            messageRepo.save(message);
        }

        return "redirect:/user-messages/" + user;
    }

    @GetMapping("/message/{message}/like")
    public String like(
            @AuthenticationPrincipal User currentUser,
            @PathVariable Message message,
            RedirectAttributes redirectAttributes,
            @RequestHeader(required = false) String referer
    ) {
        Set<User> likes = message.getLikes();

        if (likes.contains(currentUser))
            likes.remove(currentUser);
        else
            likes.add(currentUser);

        UriComponents components = UriComponentsBuilder.fromHttpUrl(referer).build();
        components.getQueryParams()
                .entrySet()
                .forEach(pair -> redirectAttributes.addAttribute(pair.getKey(), pair.getValue()));

        messageRepo.save(message);

        return "redirect:" + components.getPath();
    }

}