package kirillzhdanov.identityservice.model.tags;

import jakarta.persistence.*;
import kirillzhdanov.identityservice.model.Brand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "group_tags")
@Getter
@Setter
@NoArgsConstructor
public class GroupTag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private GroupTag parent;

    @Column(nullable = false)
    private String path = "/";

    @Column(nullable = false)
    private int level = 0;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("name ASC")
    private Set<GroupTag> children = new HashSet<>();

    public GroupTag(String name, Brand brand, GroupTag parent) {
        this.name = name;
        this.brand = brand;
        this.setParent(parent);
    }

    public void setParent(GroupTag parent) {
        this.parent = parent;
        if (parent != null) {
            this.path = parent.getPath() + parent.getId() + "/";
            this.level = parent.getLevel() + 1;
        } else {
            this.path = "/";
            this.level = 0;
        }
        updateChildrenPaths();
    }

    private void updateChildrenPaths() {
        for (GroupTag child : children) {
            child.path = this.path + this.id + "/";
            child.level = this.level + 1;
            child.updateChildrenPaths();
        }
    }
}
