package com.project.kanbanService.service;

import com.project.kanbanService.domain.Board;
import com.project.kanbanService.domain.Stage;
import com.project.kanbanService.domain.Task;
import com.project.kanbanService.domain.User;
import com.project.kanbanService.exception.*;
import com.project.kanbanService.proxy.AuthenticationProxy;
import com.project.kanbanService.proxy.EmailProxy;
import com.project.kanbanService.proxy.NotificationProxy;
import com.project.kanbanService.repository.KanbanRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class KanbanServiceImpl implements IKanbanService {

    private final KanbanRepository kanbanRepository;
    private NotificationProxy notificationProxy;
    private AuthenticationProxy authenticationProxy;
    private EmailProxy emailProxy;

    @Autowired
    public KanbanServiceImpl(KanbanRepository kanbanRepository, NotificationProxy notificationProxy, AuthenticationProxy authenticationProxy, EmailProxy emailProxy) {
        this.kanbanRepository = kanbanRepository;
        this.notificationProxy = notificationProxy;
        this.authenticationProxy = authenticationProxy;
        this.emailProxy = emailProxy;
    }

    public Board getBoardFromUser(User user, int boardId) throws BoardNotFoundException
    {
        if(user.getBoards() == null || user.getBoards().isEmpty())
            throw new BoardNotFoundException();
        for (Board board : user.getBoards()) {
            if(boardId == board.getBoardId())
                return board;
        }
        throw new BoardNotFoundException();
    }
    public Stage getStageFromBoard(Board board, int stageId) throws StageNotFoundException{
        if(board.getStages() == null || board.getStages().isEmpty())
            throw new StageNotFoundException();
        for(Stage stage: board.getStages()) {
            if(stage.getStageId() == stageId)
                return stage;
        }
        throw new StageNotFoundException();
    }
    public Board createNewBoard(Board board) {
        Stage stage1 = new Stage(1, "To Be Done");
        Stage stage2 = new Stage(2, "In Progress");
        Stage stage3 = new Stage(3, "Backlog");
        Stage stage4 = new Stage(4, "Completed");
        board.setStages(Arrays.asList(stage1, stage2, stage3, stage4));
        return board;
    }

    public void giveMemberAccess(String[] members, String email) {
        Set<String> uniqueNames = new HashSet<>();
        for (String memberName : members) {
            if (memberName.isEmpty())
                continue;
            if (uniqueNames.add(memberName)) //this will add only unique name of member so we can avoid repeatation
            {
                User member = new User();
                member.setUserName(memberName); //set member Username
                member.setCreatorEmail(email); //set member CreatorEmail, but they still don't have email and password
                authenticationProxy.saveUserInMysql(member);
            }
        }
    }

    public void editMembersAccess(Board board, Board updatedBoard, User user) {
        giveMemberAccess(updatedBoard.getMembers(), user.getCreatorEmail());

        Set<String> updatedMembersSet = new HashSet<>(List.of(updatedBoard.getMembers()));
        for (String member : board.getMembers()) {
            if (member.isEmpty())
                continue;
            if (!updatedMembersSet.contains(member) && !member.equals(user.getUserName())) {
                User removeUser = new User();
                removeUser.setUserName(member);
                removeUser.setCreatorEmail(user.getCreatorEmail());
                authenticationProxy.deleteUserInMysql(removeUser);
            }
        }
    }
    @Override
    public User saveUser(User user) throws UserAlreadyExistsException {
        if(kanbanRepository.findById(user.getEmail()).isPresent())
            throw new UserAlreadyExistsException();

        return kanbanRepository.save(user);
    }

    @Override
    public Board saveBoard(Board board, String email) throws BoardAlreadyExistsException, UserNotFoundException {
        board = createNewBoard(board);

        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        if(user.getBoards() == null)
            user.setBoards(Arrays.asList(board));
        else {
            List<Board> boardList = user.getBoards();
            for (Board board1 : boardList) {
                if(board1.getBoardId() == board.getBoardId())
                    throw new BoardAlreadyExistsException();
            }
            boardList.add(board);
            user.setBoards(boardList);
        }

        kanbanRepository.save(user);
        if (board.getMembers().length >= 1)
            giveMemberAccess(board.getMembers(), email);
        return board;
    }

    @Override
    public Board saveStage(Stage stage, int boardId, String email) throws StageAlreadyExistsException, BoardNotFoundException, UserNotFoundException {
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        Board board = getBoardFromUser(user, boardId);

        if(board.getStages() == null)
            board.setStages(Arrays.asList(stage));
        else {
            List<Stage> stageList = board.getStages();
            for (Stage stage1 : stageList) {
                if(stage1.getStageId() == stage.getStageId())
                    throw new StageAlreadyExistsException();
            }
            stageList.add(stage);
            board.setStages(stageList);
        }

        kanbanRepository.save(user);
        return board;
    }

    @Override
    public Board saveTask(Task task, int boardId, int stageId, String email) throws TaskAlreadyExistsException, UserNotFoundException,  BoardNotFoundException, StageNotFoundException {
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        Board board = getBoardFromUser(user, boardId);
        Stage stage = getStageFromBoard(board, stageId);

        if(stage.getTasks() == null )
            stage.setTasks(Arrays.asList(task));
        else {
            List<Task> taskList = stage.getTasks();
            for (Task task1 : taskList) {
                if(task1.getTaskId() == task.getTaskId())
                    throw new TaskAlreadyExistsException();
            }
            taskList.add(task);
            stage.setTasks(taskList);
        }

        kanbanRepository.save(user);
        return board;
    }

    @Override
    public User getUser(String email) throws UserNotFoundException {
        return kanbanRepository.findById(email).orElseThrow(UserNotFoundException::new);
    }

    @Override
    public Board getBoard(int boardId, String email) throws BoardNotFoundException, UserNotFoundException {
        User user =  getUser(email);
        return getBoardFromUser(user, boardId);
    }

    @Override
    public Task getTask(int taskId, int stageId, int boardId, String email) throws TaskNotFoundException, UserNotFoundException, BoardNotFoundException, StageNotFoundException {
        Board board = getBoard(boardId, email);
        Stage stage = getStageFromBoard(board , stageId);

        if (stage.getTasks() == null || stage.getTasks().isEmpty())
            throw new TaskNotFoundException();

        List<Task> tasKList = stage.getTasks();
        for(Task task : tasKList) {
            if(task.getTaskId() == taskId)
                return task;
        }
        throw new TaskNotFoundException();
    }

    @Override
    public User deleteBoard(int boardId, String email) throws BoardNotFoundException, UserNotFoundException {
        boolean boardDeleted = false;
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        if(user.getBoards()==null || user.getBoards().isEmpty())
            throw new BoardNotFoundException();

        List<Board> boardList = user.getBoards();
        Iterator<Board> boardIterator = boardList.iterator();
        while(boardIterator.hasNext()) {
            if(boardIterator.next().getBoardId() == boardId){
                boardIterator.remove();
                boardDeleted=true;
            }
        }
        if(!boardDeleted)
            throw new BoardNotFoundException();

        kanbanRepository.save(user);
        return kanbanRepository.findById(email).get();
    }

    @Override
    public Board deleteStage(int stageId, int boardId, String email) throws StageNotFoundException, UserNotFoundException, BoardNotFoundException {
        boolean stageDeleted = false;
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        Board board = getBoardFromUser(user, boardId);
        if(board.getStages()==null || board.getStages().isEmpty())
            throw new StageNotFoundException();

        List<Stage> stageList = board.getStages();
        Iterator<Stage> stageIterator = stageList.iterator();
        while(stageIterator.hasNext()){
            if(stageIterator.next().getStageId() == stageId){
                stageIterator.remove();
                stageDeleted=true;
            }
        }
        if(!stageDeleted)
            throw new StageNotFoundException();

        kanbanRepository.save(user);
        return board;
    }

    @Override
    public Board deleteTask(int taskId, int stageId, int boardId, String email) throws TaskNotFoundException, UserNotFoundException, BoardNotFoundException, StageNotFoundException {
        boolean taskDeleted = false;
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        Board board = getBoardFromUser(user, boardId);
        Stage stage = getStageFromBoard(board, stageId);
        if(stage.getTasks()==null || stage.getTasks().isEmpty())
            throw new TaskNotFoundException();

        List<Task> taskList = stage.getTasks();
        Iterator<Task> taskIterator = taskList.iterator();
        while(taskIterator.hasNext()) {
            if(taskIterator.next().getTaskId() == taskId){
                taskIterator.remove();
                taskDeleted=true;
            }
        }
        if(!taskDeleted)
            throw new StageNotFoundException();

        kanbanRepository.save(user);
        return board;
    }

    @Override
    public Board updateBoard(Board updatedBoard, String email) throws BoardNotFoundException, UserNotFoundException {
        boolean boardUpdated = false;
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        if(user.getBoards()==null || user.getBoards().isEmpty())
            throw new BoardNotFoundException();

        List<Board> boardList = user.getBoards();
        for (int i = 0; i < boardList.size(); i++) {
            Board board = boardList.get(i);
            if (board.getBoardId() == updatedBoard.getBoardId()) {
                updatedBoard.setCreator(board.getCreator()); 
                updatedBoard.setCreationDate(new Date());
                updatedBoard.setStages(board.getStages());
                if (updatedBoard.getMembers() != null && updatedBoard.getMembers().length >=1)
                    editMembersAccess(board, updatedBoard, user);

                boardList.set(i, updatedBoard);
                boardUpdated = true;
                break;
            }
        }
        if(!boardUpdated)
            throw new BoardNotFoundException();

        kanbanRepository.save(user);
        return updatedBoard;
    }

    @Override
    public Board updateStage(Stage updatedStage, int boardId, String email) throws StageNotFoundException, UserNotFoundException, BoardNotFoundException {
        boolean stageUpdated = false;
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        Board board = getBoardFromUser(user, boardId);
        if(board.getStages()==null || board.getStages().isEmpty())
            throw new StageNotFoundException();

        List<Stage> stageList = board.getStages();
        for (int i = 0; i < stageList.size(); i++) {
            Stage stage = stageList.get(i);
            if (stage.getStageId() == updatedStage.getStageId()) {
                updatedStage.setTasks(stage.getTasks());
                stageList.set(i, updatedStage);
                stageUpdated = true;
                break;
            }
        }
        if(!stageUpdated)
            throw new StageNotFoundException();

        kanbanRepository.save(user);
        return board;
    }

    @Override
    public Board updateTask(Task updatedTask, int stageId, int boardId, String email) throws TaskNotFoundException, UserNotFoundException, BoardNotFoundException, StageNotFoundException {
        boolean taskUpdated = false;
        if(kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        Board board = getBoardFromUser(user, boardId);
        Stage stage = getStageFromBoard(board, stageId);
        if(stage.getTasks()==null || stage.getTasks().isEmpty())
            throw new TaskNotFoundException();

        List<Task> taskList = stage.getTasks();
        for (int i = 0; i < taskList.size(); i++) {
            Task task = taskList.get(i);
            if (task.getTaskId() == updatedTask.getTaskId()) {
                taskList.set(i, updatedTask);
                taskUpdated = true;
                break;
            }
        }
        if(!taskUpdated)
            throw new StageNotFoundException();

        kanbanRepository.save(user);
        return board;
    }

    @Override
    public Board moveTask(int taskId, int boardId, int currentStageId, int newStageId, String email) throws WrongMoveException, DataMissingException, UserNotFoundException, StageNotFoundException, TaskNotFoundException, BoardNotFoundException, TaskAlreadyExistsException {
        Task taskToMove = getTask(taskId, currentStageId, boardId, email);
        String assignedTo = taskToMove.getAssignee();

        if(assignedTo == null)
            throw new DataMissingException();

        if (kanbanRepository.findById(email).isEmpty())
            throw new UserNotFoundException();

        User user = kanbanRepository.findById(email).get();
        if(newStageId == 2){
            Board board = getBoardFromUser(user, boardId);
            Stage stage = getStageFromBoard(board, 2);  // stageId = 2 is for in-progress stage

            if (stage.getTasks() == null)
                stage.setTasks(new ArrayList<Task>());
            else {
                List<Task> taskList = stage.getTasks();
                int count = 0;
                for(Task task: taskList){
                    if(task.getAssignee().equals(assignedTo))
                        count++;
                }
                if(count >= 2)
                    throw new WrongMoveException();
            }
        }
        saveTask(taskToMove, boardId, newStageId, email);
        deleteTask(taskId, currentStageId, boardId, email);

        if (newStageId == 4)   {        // stageId = 4 is for completed stage
            String message = notificationProxy.saveNotification(taskToMove);
            System.out.println("Notification: "+ message);
            String taskMessage = taskToMove.getTaskName().concat(" task completed by ").concat(taskToMove.getAssignee());
            emailProxy.completedTaskConfirmation(email, taskMessage);
        }

        return getBoard(boardId, email);
    }
}
