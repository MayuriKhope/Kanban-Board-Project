package com.project.authentication.service;
import com.project.authentication.domain.User;
import com.project.authentication.exception.InvalidCredentialsException;
import com.project.authentication.exception.UserAlreadyExistException;
import com.project.authentication.proxy.EmailProxy;
import com.project.authentication.proxy.KanbanProxy;
import com.project.authentication.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements IUserService
{
    private UserRepository userRepository;
    private KanbanProxy kanbanProxy;
    private EmailProxy emailProxy;

    @Autowired
    public UserServiceImpl(UserRepository userRepository, KanbanProxy kanbanProxy, EmailProxy emailProxy) {
        this.userRepository = userRepository;
        this.kanbanProxy = kanbanProxy;
        this.emailProxy = emailProxy;
    }

    @Override
    public User registerUser(User user)throws UserAlreadyExistException
    {
        if (user.getEmail() == null || user.getEmail().isEmpty()) {
            if (userRepository.findByUserNameAndCreatorEmail(user.getUserName(), user.getCreatorEmail()) != null)
                return user;
            String email = user.getUserName().concat("@gmail.com");
            String password = user.getUserName().concat("*Ps1");
            user.setEmail(email);
            user.setPassword(password);
            emailProxy.confirmRegisterForMember(user);//send mail to member
            return userRepository.save(user);
        }

        if(userRepository.findById(user.getEmail()).isPresent())
           throw new UserAlreadyExistException();

        if (user.getCreatorEmail() == null || user.getCreatorEmail().isEmpty())
            user.setCreatorEmail(user.getEmail());

        kanbanProxy.saveUserInMongoDb(user);
        emailProxy.confirmRegisterForCreator(user);//send mail to user
        return userRepository.save(user);
    }

    @Override
    public User loginUser(String email, String password)throws InvalidCredentialsException
    {
        User loggedInUser = userRepository.findByEmailAndPassword(email, password);
        if(loggedInUser == null)
            loggedInUser = userRepository.findByUserNameAndPassword(email, password);
        if(loggedInUser == null)
            throw new InvalidCredentialsException();

        return loggedInUser;
    }

    @Override
    public void deleteUser(User user) throws InvalidCredentialsException {
        User deleteUser = userRepository.findByUserNameAndCreatorEmail(user.getUserName(), user.getCreatorEmail());
        if (deleteUser == null)
            throw new InvalidCredentialsException();

        emailProxy.memberRemovedConfirmation(deleteUser);//send mail to user
        userRepository.delete(deleteUser);
    }
}
