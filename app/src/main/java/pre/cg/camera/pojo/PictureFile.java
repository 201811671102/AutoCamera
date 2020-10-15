package pre.cg.camera.pojo;


import org.litepal.annotation.Column;
import org.litepal.crud.LitePalSupport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PictureFile extends LitePalSupport {

    @Column(index = true,unique = true,ignore = false,nullable = false)
    private int id;
    @Column(unique = true,nullable = false,index = false)
    private String url;
    @Column(unique = true,nullable = false,ignore = false)
    private String date;

    @Column(ignore = true)
    private boolean delete;
}
