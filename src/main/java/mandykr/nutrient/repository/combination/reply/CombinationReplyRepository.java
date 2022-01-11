package mandykr.nutrient.repository.combination.reply;

import mandykr.nutrient.entity.combination.Combination;
import mandykr.nutrient.entity.combination.CombinationReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CombinationReplyRepository extends JpaRepository<CombinationReply, Long> {
    Page<CombinationReply> findByCombinationAndOrders(Combination combination, long orders, Pageable pageable);
    Page<CombinationReply> findByParent(CombinationReply parent, Pageable pageable);
}
