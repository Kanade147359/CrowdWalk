// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Area.MapArea;

/**
 * NetworkMap �ι������Ǥξ����Ѳ���ƻ뤹��ꥹ���ѥ��󥿡��ե�����.
 *
 * ���ߥ�졼�������̤����蹹���ѡ�<br />
 * ���θ��Υ���åɤ���ľ�ܸƤФ�뤿�ᡢ���֤γݤ�������Ϥ��ʤ����ȡ�
 */
public interface NetworkMapPartsListener {
    /**
     * ��󥯤�������줿.
     */
    public void linkRemoved(MapLink link);

    /**
     * ��󥯥������ɲä��줿.
     */
    public void linkTagAdded(MapLink link, String tag);

    /**
     * ��󥯥�����������줿.
     */
    public void linkTagRemoved(MapLink link);

    /**
     * �Ρ��ɥ������ɲä��줿.
     */
    public void nodeTagAdded(MapNode node, String tag);

    /**
     * �Ρ��ɥ�����������줿.
     */
    public void nodeTagRemoved(MapNode node);

    /**
     * Pollution ��٥뤬�Ѳ�����.
     */

    public void pollutionLevelChanged(MapArea area);

    /**
     * ����������Ȥ��ɲä��줿.
     */
    public void agentAdded(AgentBase agent);

    /**
     * ����������Ȥ���ư����(swing ��ޤ�).
     */
    public void agentMoved(AgentBase agent);

    /**
     * ����������ȤΥ��ԡ��ɤ��Ѳ�����.
     */
    public void agentSpeedChanged(AgentBase agent);

    /**
     * ����������ȤΥȥꥢ������٥뤬�Ѳ�����.
     */
    public void agentTriageChanged(AgentBase agent);

    /**
     * ����������Ȥ����񤬴�λ����.
     */
    public void agentEvacuated(AgentBase agent);
}
