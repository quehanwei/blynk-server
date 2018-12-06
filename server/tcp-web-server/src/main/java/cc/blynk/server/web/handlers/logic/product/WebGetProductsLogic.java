package cc.blynk.server.web.handlers.logic.product;

import cc.blynk.server.Holder;
import cc.blynk.server.core.dao.OrganizationDao;
import cc.blynk.server.core.model.auth.User;
import cc.blynk.server.core.model.permissions.Role;
import cc.blynk.server.core.model.serialization.JsonParser;
import cc.blynk.server.core.model.web.Organization;
import cc.blynk.server.core.protocol.model.messages.StringMessage;
import cc.blynk.server.core.session.web.WebAppStateHolder;
import cc.blynk.server.core.PermissionBasedLogic;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static cc.blynk.server.core.model.dto.ProductDTO.toDTO;
import static cc.blynk.server.core.model.permissions.PermissionsTable.PRODUCT_VIEW;
import static cc.blynk.server.internal.CommonByteBufUtil.makeUTF8StringMessage;
import static cc.blynk.server.internal.WebByteBufUtil.json;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 07.06.18.
 */
public class WebGetProductsLogic implements PermissionBasedLogic {

    private static final Logger log = LogManager.getLogger(WebGetProductsLogic.class);

    private final OrganizationDao organizationDao;

    public WebGetProductsLogic(Holder holder) {
        this.organizationDao = holder.organizationDao;
    }

    @Override
    public boolean hasPermission(Role role) {
        return role.canViewProduct();
    }

    @Override
    public int getPermission() {
        return PRODUCT_VIEW;
    }

    @Override
    public void messageReceived0(ChannelHandlerContext ctx, WebAppStateHolder state, StringMessage message) {
        User user = state.user;
        int orgId;
        if (message.body.isEmpty()) {
            orgId = user.orgId;
        } else {
            orgId = Integer.parseInt(message.body);
        }

        Organization organization = organizationDao.getOrgByIdOrThrow(orgId);

        if (organization == null) {
            log.error("Cannot find org with id {} for user {}", user.orgId, user.email);
            ctx.writeAndFlush(json(message.id, "Cannot find organization."), ctx.voidPromise());
            return;
        }

        String productString = JsonParser.toJson(toDTO(organization.products));
        if (productString == null) {
            log.error("Empty response for WebGetProductsLogic and {}.", user.email);
        } else {
            log.trace("Returning products for user {} and orgId {}, length {}.",
                    user.email, user.orgId, productString.length());
            StringMessage response = makeUTF8StringMessage(message.command, message.id, productString);
            ctx.writeAndFlush(response, ctx.voidPromise());
        }
    }

}
