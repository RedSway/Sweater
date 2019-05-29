package com.example.sweater.service;

import com.example.sweater.domain.User;
import com.example.sweater.domain.dto.MessageDto;
import com.example.sweater.repos.MessageRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


@Service
public class MessageService {
    private final MessageRepo messageRepo;

    public Page<MessageDto> messageList(Pageable pageable, String filter, User user) {
        if (filter != null && !filter.isEmpty())
            return messageRepo.findByTag(filter, pageable, user);
        else
            return messageRepo.findAll(pageable, user);
    }

    @Autowired
    public MessageService(MessageRepo messageRepo) {
        this.messageRepo = messageRepo;
    }

    public Page<MessageDto> messageListForUser(Pageable pageable, User author, User currentUser) {
        return messageRepo.findByUser(pageable, author, currentUser);
    }
}
