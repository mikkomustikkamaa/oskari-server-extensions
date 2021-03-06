package flyway.asdi;

import fi.nls.oskari.domain.map.view.Bundle;
import fi.nls.oskari.domain.map.view.View;
import fi.nls.oskari.domain.map.view.ViewTypes;
import fi.nls.oskari.log.LogFactory;
import fi.nls.oskari.log.Logger;
import fi.nls.oskari.map.view.AppSetupServiceMybatisImpl;
import fi.nls.oskari.map.view.ViewService;
import fi.nls.oskari.util.JSONHelper;
import fi.nls.oskari.view.modifier.ViewModifier;
import org.flywaydb.core.api.migration.jdbc.JdbcMigration;
import org.json.JSONObject;

import java.sql.Connection;
import java.util.List;

/**
 * Created by SMAKINEN on 24.2.2017.
 */
public class V1_5_5__Add_mapwfs2_to_mapfull_imports implements JdbcMigration {

    private static final Logger LOG = LogFactory.getLogger(V1_5_5__Add_mapwfs2_to_mapfull_imports.class);

    private ViewService service;
    private static final int BATCH_SIZE = 50;
    private int updatedViewCount = 0;

    public void migrate(Connection connection)
            throws Exception {
        service = new AppSetupServiceMybatisImpl();
        int page = 1;
        while(updateViews(page)) {
            page++;
        }
        LOG.info("Updated views:", updatedViewCount);
    }
    private boolean updateViews(int page)
            throws Exception {
        List<View> list = service.getViews(page, BATCH_SIZE);
        LOG.info("Got", list.size(), "views on page", page);
        for(View view : list) {
            final Bundle mapfull = view.getBundleByName(ViewModifier.BUNDLE_MAPFULL);
            // update mapOptions

            JSONObject startup = JSONHelper.createJSONObject(mapfull.getStartup());
            JSONObject imports = startup.optJSONObject("metadata").optJSONObject("Import-Bundle");
            JSONObject mapwfs = imports.optJSONObject("mapwfs2");
            if(mapwfs == null) {
                mapwfs = new JSONObject();
                if(isOl2(view.getType())) {
                    // ol2
                    mapwfs.put("bundlePath", "/Oskari/packages/framework/bundle/");
                } else {
                    // ol3
                    mapwfs.put("bundlePath", "/Oskari/packages/mapping/ol3/");
                }
                imports.put("mapwfs2", mapwfs);
            }
            mapfull.setStartup(startup.toString(2));
            service.updateBundleSettingsForView(view.getId(), mapfull);
            updatedViewCount++;
        }
        return list.size() == BATCH_SIZE;
    }

    private boolean isOl2(String type) {
        return !type.equalsIgnoreCase(ViewTypes.PUBLISHED) && !type.equalsIgnoreCase(ViewTypes.PUBLISH_TEMPLATE);
    }
}
