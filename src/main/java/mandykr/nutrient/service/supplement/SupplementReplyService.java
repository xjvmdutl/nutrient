package mandykr.nutrient.service.supplement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mandykr.nutrient.dto.supplement.reply.SupplementReplyDto;
import mandykr.nutrient.dto.supplement.reply.request.SupplementReplyRequest;
import mandykr.nutrient.entity.member.Member;
import mandykr.nutrient.entity.supplement.Supplement;
import mandykr.nutrient.entity.supplement.SupplementReply;
import mandykr.nutrient.repository.supplement.SupplementRepository;
import mandykr.nutrient.repository.supplement.reply.SupplementReplyRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityNotFoundException;
import java.util.Optional;

import static org.springframework.data.domain.Sort.by;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SupplementReplyService {

    private final SupplementReplyRepository supplementReplyRepository;

    private final SupplementRepository supplementRepository;

    public Page<SupplementReplyDto> getSupplementRepliesWithParent(Long supplementId, Pageable pageable){
        Supplement supplement = getSupplement(supplementId);
        return supplementReplyRepository
                .findBySupplementWithParent(supplement, pageable)
                .map(SupplementReplyDto::new);
    }

    public Page<SupplementReplyDto> getSupplementRepliesWithChild(Long supplementId, Long supplementReplyId, Pageable pageable){
        Supplement supplement = getSupplement(supplementId);
        SupplementReply supplementReply = getSupplementReply(supplementReplyId);
        return supplementReplyRepository.findBySupplementWithChild(supplement, supplementReply, pageable)
            .map(SupplementReplyDto::new);
    }

    @Transactional
    public SupplementReplyDto createSupplementReply(Long supplementId, Member member, SupplementReplyRequest supplementReplyRequest){
        //첫 댓글
        Supplement supplement = getSupplement(supplementId);
        Long lastGroup = isNull(supplementReplyRepository.findByParentLastGroup(supplementId));

        return Optional.of(
                supplementReplyRepository.save(
                    SupplementReply.builder()
                    .content(supplementReplyRequest.getContent())
                    .groups(lastGroup)
                    .groupOrder(1L)
                    .member(member)
                    .deleted(false)
                    .parent(null)
                    .supplement(supplement)
                    .build()
                )
            ).map(SupplementReplyDto::new)
            .get();
    }



    @Transactional
    public SupplementReplyDto createSupplementReply(Long supplementId, Long supplementReplyId, Member member, SupplementReplyRequest supplementReplyRequest) {
        //대댓글
        Supplement supplement = getSupplement(supplementId);
        SupplementReply supplementReply = getSupplementReply(supplementReplyId);

        Long groupOrder = isNull(supplementReplyRepository.findByChildLastGroupOrder(supplementId, supplementReplyId));
        SupplementReply saveSupplementReply = supplementReplyRepository.save(SupplementReply.builder()
                                                .content(supplementReplyRequest.getContent())
                                                .groups(supplementReply.getGroups())
                                                .groupOrder(groupOrder+1)
                                                .deleted(false)
                                                .supplement(supplement)
                                                .build());
        saveSupplementReply.addParents(supplementReply);
        return Optional.of(saveSupplementReply).map(SupplementReplyDto::new).get();
    }





    public SupplementReplyDto updateSupplementReply(Long supplementReplyId, Member member, SupplementReplyRequest supplementReplyRequest){
        //변경감지
        SupplementReply findSupplementReply = getSupplementReplyWithMember(supplementReplyId, member);
        findSupplementReply.changeContent(supplementReplyRequest.getContent());
        return Optional.of(findSupplementReply).map(SupplementReplyDto::new).get();
    }

    @Transactional
    public void deleteSupplementReply(Long supplementReplyId, Member member){
        //DEPTH 2 그냥 삭제
        //      대댓글을 지웠을때, 부모도 삭제상태이고,대댓글을 마지막이 나였다면 부모도 지워
        //DEPTH 1 대댓글 여부 확인
        SupplementReply supplementReply = getSupplementReplyWithMember(supplementReplyId, member);

        if(!supplementReply.parentIsNull()){
            SupplementReply parent = supplementReply.getParent();
            supplementReplyRepository.deleteById(supplementReplyId);
            parent.deleteChild(supplementReply);
            if(parent.isDeleted() && parent.childIsEmpty()){
                supplementReplyRepository.deleteById(parent.getId());
            }
        }else{
            if(supplementReply.childIsEmpty()){ //대댓글 없다면 지워
                supplementReplyRepository.deleteById(supplementReplyId);
            }else{
                //대댓글 있다면 flag만 변경
                //변경감지 활용
                supplementReply.delete();
            }
        }

    }
    private Supplement getSupplement(Long supplementId) {
        return supplementRepository.findById(supplementId)
                .orElseThrow(() -> new EntityNotFoundException("not found Supplement : " + supplementId));
    }
    private SupplementReply getSupplementReply(Long supplementReplyId){
        return supplementReplyRepository.findById(supplementReplyId)
                .orElseThrow(() -> new EntityNotFoundException("not found SupplementReply : " + supplementReplyId));
    }
    private SupplementReply getSupplementReplyWithMember(Long supplementReplyId, Member member) {
        return supplementReplyRepository.findByIdAndMember(supplementReplyId, member)
                .orElseThrow(() -> new EntityNotFoundException("not found SupplementReply : " + supplementReplyId));
    }
    private Long isNull(Long value) {
        return value == null ? 0L : value;
    }
}
