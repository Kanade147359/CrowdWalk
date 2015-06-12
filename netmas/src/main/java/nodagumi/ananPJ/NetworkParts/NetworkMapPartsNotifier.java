// -*- mode: java; indent-tabs-mode: nil -*-
package nodagumi.ananPJ.NetworkParts;

import java.util.ArrayList;

import nodagumi.ananPJ.Agents.AgentBase;
import nodagumi.ananPJ.NetworkMap;
import nodagumi.ananPJ.NetworkParts.Link.MapLink;
import nodagumi.ananPJ.NetworkParts.Node.MapNode;
import nodagumi.ananPJ.NetworkParts.Area.MapArea;

/**
 * NetworkMap �ι������Ǥξ����Ѳ������Τ���.
 *
 * ���٥�Ȥϻ��Ѥ����������ѤΥ᥽�åɤ���ľ�ܥꥹ�ʤΥ᥽�åɤ�ƤӽФ��Ƥ��롣
 */
public class NetworkMapPartsNotifier {
    private NetworkMap map;
    private ArrayList<NetworkMapPartsListener> listeners = new ArrayList();

    public NetworkMapPartsNotifier(NetworkMap map) {
        this.map = map;
    }

    public NetworkMap getNetworkMap() {
        return map;
    }

    /**
     * �ꥹ�ʤ���Ͽ����.
     */
    public synchronized void addListener(NetworkMapPartsListener listener) {
        listeners.add(listener);
    }

    /**
     * ��󥯤�������줿�������Τ���.
     */
    public void linkRemoved(MapLink link) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.linkRemoved(link);
        }
    }

    /**
     * ��󥯥������ɲä��줿�������Τ���.
     */
    public void linkTagAdded(MapLink link, String tag) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.linkTagAdded(link, tag);
        }
    }

    /**
     * ��󥯥�����������줿�������Τ���.
     */
    public void linkTagRemoved(MapLink link) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.linkTagRemoved(link);
        }
    }

    /**
     * �Ρ��ɥ������ɲä��줿�������Τ���.
     */
    public void nodeTagAdded(MapNode node, String tag) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.nodeTagAdded(node, tag);
        }
    }

    /**
     * �Ρ��ɥ�����������줿�������Τ���.
     */
    public void nodeTagRemoved(MapNode node) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.nodeTagRemoved(node);
        }
    }

    /**
     * Pollution ��٥뤬�Ѳ������������Τ���.
     */
    public void pollutionLevelChanged(MapArea area) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.pollutionLevelChanged(area);
        }
    }

    /**
     * ����������Ȥ��ɲä��줿�������Τ���.
     */
    public void agentAdded(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentAdded(agent);
        }
    }

    /**
     * ����������Ȥ���ư(swing ��ޤ�)�����������Τ���.
     */
    public void agentMoved(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentMoved(agent);
        }
    }

    /**
     * ����������ȤΥ��ԡ��ɤ��Ѳ������������Τ���.
     */
    public void agentSpeedChanged(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentSpeedChanged(agent);
        }
    }

    /**
     * ����������ȤΥȥꥢ������٥뤬�Ѳ������������Τ���.
     */
    public void agentTriageChanged(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentTriageChanged(agent);
        }
    }

    /**
     * ����������Ȥ����񤬴�λ�����������Τ���.
     */
    public void agentEvacuated(AgentBase agent) {
        for (NetworkMapPartsListener listener : listeners) {
            listener.agentEvacuated(agent);
        }
    }
}
