package pan.alexander.tordnscrypt.installer;

/*
    This file is part of Fulldive VPN.

    Fulldive VPN is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    Fulldive VPN is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with Fulldive VPN.  If not, see <http://www.gnu.org/licenses/>.

    Copyright 2019-2021 by Garmatin Oleksandr invizible.soft@gmail.com
*/

import android.content.Context;

import pan.alexander.tordnscrypt.utils.zipUtil.ZipFileManager;

public class DNSCryptExtractCommand extends AssetsExtractCommand {
    private final String appDataDir;

    DNSCryptExtractCommand(Context context, String appDataDir) {
        super(context);
        this.appDataDir = appDataDir;
    }

    @Override
    public void execute() throws Exception {
        ZipFileManager zipFileManager = new ZipFileManager();
        zipFileManager.extractZipFromInputStream(assets.open("dnscrypt.mp3"), appDataDir);
    }
}
