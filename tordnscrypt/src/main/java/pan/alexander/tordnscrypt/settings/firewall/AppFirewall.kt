package pan.alexander.tordnscrypt.settings.firewall

/*
    This file is part of VPN.

    VPN is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    VPN is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with VPN.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import pan.alexander.tordnscrypt.settings.tor_apps.ApplicationData

data class AppFirewall(val applicationData: ApplicationData,
                       var allowLan: Boolean,
                       var allowWifi: Boolean,
                       var allowGsm: Boolean,
                       var allowRoaming: Boolean,
                       var allowVPN: Boolean)
