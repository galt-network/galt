(ns galt.comments.domain.entities.comment)

(defn nest-comments [comments]
  (let [by-parent (group-by :parent-id comments)
        id-set (set (map :id comments))
        top-level (filter #(or (nil? (:parent-id %))
                               (not (contains? id-set (:parent-id %))))
                          comments)
        build (fn build [comment]
                (let [children (get by-parent (:id comment) [])]
                  (assoc comment :replies (map build children))))]
    (map build top-level)))

(comment
  (nest-comments [{:id 1 :parent-id nil :content "Top level"}
                  {:id 2 :parent-id 1 :content "A reply"}
                  {:id 3 :parent-id 2 :content "Reply to a reply"}])
  (group-by :parent-id [{:id 3 :parent-id 2 :content "Reply to a reply"}])
  (nest-comments [{:id 3 :parent-id 2 :content "Reply to a reply"}]))
