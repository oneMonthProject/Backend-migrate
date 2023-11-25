package com.example.demo.service.board;

import com.example.demo.constant.ProjectMemberStatus;
import com.example.demo.constant.UserProjectHistoryStatus;
import com.example.demo.dto.board.request.BoardSearchRequestDto;
import com.example.demo.dto.board.response.*;
import com.example.demo.dto.board_project.request.BoardProjectCreateRequestDto;
import com.example.demo.dto.board_project.request.BoardProjectUpdateRequestDto;
import com.example.demo.dto.board_project.response.BoardProjectCreateResponseDto;
import com.example.demo.dto.board_project.response.BoardProjectUpdateResponseDto;
import com.example.demo.dto.boardposition.BoardPositionDetailResponseDto;
import com.example.demo.dto.position.response.PositionResponseDto;
import com.example.demo.dto.project.response.ProjectCreateResponseDto;
import com.example.demo.dto.project.response.ProjectDetailResponseDto;
import com.example.demo.dto.project.response.ProjectUpdateResponseDto;
import com.example.demo.dto.trust_grade.response.TrustGradeResponseDto;
import com.example.demo.dto.user.response.UserBoardDetailResponseDto;
import com.example.demo.dto.user.response.UserProjectResponseDto;
import com.example.demo.global.exception.customexception.*;
import com.example.demo.model.board.Board;
import com.example.demo.model.board.BoardPosition;
import com.example.demo.model.position.Position;
import com.example.demo.model.project.Project;
import com.example.demo.model.project.ProjectMember;
import com.example.demo.model.project.ProjectMemberAuth;
import com.example.demo.model.trust_grade.TrustGrade;
import com.example.demo.model.user.User;
import com.example.demo.model.user.UserProjectHistory;
import com.example.demo.repository.board.BoardPositionRepository;
import com.example.demo.repository.board.BoardRepository;
import com.example.demo.repository.position.PositionRepository;
import com.example.demo.repository.project.ProjectMemberAuthRepository;
import com.example.demo.repository.project.ProjectMemberRepository;
import com.example.demo.repository.project.ProjectRepository;
import com.example.demo.repository.trust_grade.TrustGradeRepository;
import com.example.demo.repository.user.UserProjectHistoryRepository;
import com.example.demo.repository.user.UserRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.example.demo.constant.ProjectStatus.RECRUITING;

@Transactional
@Service
@AllArgsConstructor
public class BoardService {

    private final PositionRepository positionRepository;

    private final BoardRepository boardRepository;

    private final UserRepository userRepository;

    private final ProjectRepository projectRepository;

    private final BoardPositionRepository boardPositionRepository;

    private final TrustGradeRepository trustGradeRepository;
    private final ProjectMemberAuthRepository projectMemberAuthRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserProjectHistoryRepository userProjectHistoryRepository;

    /**
     * 게시글 목록 검색
     *
     * @param dto
     * @return List<BoardSearchResponseDto>
     */
    @Transactional(readOnly = true)
    public Page<BoardSearchResponseDto> search(BoardSearchRequestDto dto, Pageable pageable) {
       return boardRepository.getBoardSearchPage(dto, pageable);
    }
    
    // 게시글 상세 조회
    public BoardTotalDetailResponseDto getDetail(Long boardId) {
        Board board =
                boardRepository
                        .findById(boardId)
                        .orElseThrow(() -> BoardCustomException.NOT_FOUND_BOARD);
        UserBoardDetailResponseDto userBoardDetailResponseDto =
                UserBoardDetailResponseDto.of(board.getUser());

        List<BoardPositionDetailResponseDto> boardPositionDetailResponseDtos = new ArrayList<>();
        for (BoardPosition boardPosition : board.getPositions()) {
            PositionResponseDto positionResponseDto =
                    PositionResponseDto.of(boardPosition.getPosition());
            BoardPositionDetailResponseDto boardPositionDetailResponseDto =
                    BoardPositionDetailResponseDto.of(boardPosition, positionResponseDto);
            boardPositionDetailResponseDtos.add(boardPositionDetailResponseDto);
        }
        BoardDetailResponseDto boardDetailResponseDto =
                BoardDetailResponseDto.of(
                        board, userBoardDetailResponseDto, boardPositionDetailResponseDtos);

        // ProjectDetailResponseDto 부분
        TrustGradeResponseDto trustGradeDto = TrustGradeResponseDto.of(board.getProject().getTrustGrade());
        UserProjectResponseDto userProjectResponseDto =
                UserProjectResponseDto.of(board.getProject());
        ProjectDetailResponseDto projectDetailResponseDto =
                ProjectDetailResponseDto.of(
                        board.getProject(), trustGradeDto, userProjectResponseDto);

        BoardTotalDetailResponseDto boardTotalDetailResponseDto =
                BoardTotalDetailResponseDto.of(boardDetailResponseDto, projectDetailResponseDto);

        return boardTotalDetailResponseDto;
    }

    /**
     * 게시글, 프로젝트 생성 , 프로젝트 멤버 생성, 사용자 이력 생성, 게시글-포지션 생성
     * @param dto
     * @return
     */
    public BoardProjectCreateResponseDto create(BoardProjectCreateRequestDto dto) {
        User tempUser =
                userRepository
                        .findById(1L)
                        .orElseThrow(
                                () -> UserCustomException.NOT_FOUND_USER); // 나중에 Security로 고쳐야 함.

        TrustGrade trustGrade =
                trustGradeRepository
                        .findById(dto.getProject().getTrustGradeId())
                        .orElseThrow(() -> TrustGradeCustomException.NOT_FOUND_TRUST_GRADE);

        // project 생성
        Project project =
                Project.builder()
                        .name(dto.getProject().getName())
                        .subject(dto.getProject().getSubject())
                        .trustGrade(trustGrade)
                        .user(tempUser)
                        .status(RECRUITING)
                        .crewNumber(dto.getProject().getCrewNumber())
                        .startDate(dto.getProject().getStartDate())
                        .endDate(dto.getProject().getEndDate())
                        .build();

        Project savedProject = projectRepository.save(project);

        // board 생성
        Board board =
                Board.builder()
                        .title(dto.getBoard().getTitle())
                        .content(dto.getBoard().getContent())
                        .project(savedProject)
                        .user(tempUser)
                        .contact(dto.getBoard().getContent())
                        .build();

        Board savedBoard = boardRepository.save(board);

        // boardPosition 생성
        List<BoardPosition> boardPositionList = new ArrayList<>();
        for (Long positionId : dto.getBoard().getPositionIds()) {
            Position position =
                    positionRepository
                            .findById(positionId)
                            .orElseThrow(() -> PositionCustomException.NOT_FOUND_POSITION);
            BoardPosition boardPosition = new BoardPosition(savedBoard, position);
            boardPositionRepository.save(boardPosition);
        }
        savedBoard.setPositions(boardPositionList);

        ProjectMemberAuth projectMemberAuth = projectMemberAuthRepository.findById(1L).orElseThrow(() -> ProjectMemberAuthCustomException.NOT_FOUND_PROJECT_MEMBER_AUTH);
        ProjectMember projectMember = ProjectMember.builder()
                .project(savedProject)
                .user(tempUser)
                .projectMemberAuth(projectMemberAuth)
                .status(ProjectMemberStatus.PARTICIPATING)
                .position(savedProject.getUser().getPosition())
                .build();

        projectMemberRepository.save(projectMember);

        UserProjectHistory userProjectHistory = UserProjectHistory.builder()
                .user(tempUser)
                .project(savedProject)
                .startDate(LocalDateTime.now())
                .endDate(savedProject.getEndDate())
                .status(UserProjectHistoryStatus.PARTICIPATING)
                .build();

        userProjectHistoryRepository.save(userProjectHistory);

        // response값 생성
        BoardCreateResponseDto boardCreateResponseDto = BoardCreateResponseDto.of(board);
        ProjectCreateResponseDto projectCreateResponseDto = ProjectCreateResponseDto.of(project);

        return new BoardProjectCreateResponseDto(boardCreateResponseDto, projectCreateResponseDto);
    }

    /**
     * 게시글, 프로젝트 업데이트
     * TODO : 현재 유저가 업데이트 하도록 변경
     * @param dto
     * @return
     */
    public BoardProjectUpdateResponseDto update(Long boardId, BoardProjectUpdateRequestDto dto) {
        Project project = projectRepository.findById(dto.getProject().getProjectId()).get();
        User tempUser = userRepository.findById(1L).get(); // 나중에 Security로 고쳐야 함.

        TrustGrade trustGrade =
                trustGradeRepository
                        .findById(dto.getProject().getProjectTrustId())
                        .orElseThrow(() -> TrustGradeCustomException.NOT_FOUND_TRUST_GRADE);

        // project 생성
        project =
                Project.builder()
                        .name(dto.getProject().getProjectName())
                        .subject(dto.getProject().getProjectSubject())
                        .trustGrade(trustGrade)
                        .user(project.getUser())
                        .status(project.getStatus())
                        .crewNumber(dto.getProject().getProjectCrewNumber())
                        .startDate(dto.getProject().getProjectStartDate())
                        .endDate(dto.getProject().getProjectEndDate())
                        .build();

        Project savedProject = projectRepository.save(project);

        // board 생성
        Board board =
                boardRepository
                        .findById(boardId)
                        .orElseThrow(() -> BoardCustomException.NOT_FOUND_BOARD);

        board =
                Board.builder()
                        .title(dto.getBoard().getTitle())
                        .content(dto.getBoard().getContent())
                        .project(board.getProject())
                        .user(board.getUser())
                        .contact(dto.getBoard().getContent())
                        .build();

        Board savedBoard = boardRepository.save(board);

        // position 받아서 다시 보드-포지션 연결
        List<BoardPosition> boardPositionList = new ArrayList<>();
        for (Long positionId : dto.getBoard().getPositions()) {
            Position position =
                    positionRepository
                            .findById(positionId)
                            .orElseThrow(() -> PositionCustomException.NOT_FOUND_POSITION);
            BoardPosition boardPosition = new BoardPosition(savedBoard, position);
            // boardPositionRepository.save(boardPosition);
            boardPositionList.add(boardPosition);
        }
        savedBoard.setPositions(boardPositionList);


        // response값 생성
        BoardUpdateResponseDto boardUpdateResponseDto = BoardUpdateResponseDto.of(board);
        ProjectUpdateResponseDto projectUpdateResponseDto = ProjectUpdateResponseDto.of(project);

        return new BoardProjectUpdateResponseDto(boardUpdateResponseDto, projectUpdateResponseDto);
    }
}
