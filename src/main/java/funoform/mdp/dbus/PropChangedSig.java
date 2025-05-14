package funoform.mdp.dbus;

import java.util.List;
import java.util.Map;

import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties.PropertiesChanged;
import org.freedesktop.dbus.types.Variant;

public class PropChangedSig extends PropertiesChanged{

	public PropChangedSig(String _path, String _interfaceName, Map<String, Variant<?>> _propertiesChanged,
			List<String> _propertiesRemoved) throws DBusException {
		super(_path, _interfaceName, _propertiesChanged, _propertiesRemoved);
	}

}
