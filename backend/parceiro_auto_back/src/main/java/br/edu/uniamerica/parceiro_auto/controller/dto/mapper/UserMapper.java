package br.edu.uniamerica.parceiro_auto.controller.dto.mapper;

import br.edu.uniamerica.parceiro_auto.controller.dto.user.UserResponseDTO;
import br.edu.uniamerica.parceiro_auto.entity.User;

public class UserMapper {
    public static UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(user.getId(), user.getLogin());
    }
}
