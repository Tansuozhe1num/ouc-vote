package com.vote.server;

import com.vote.data.po.SnapInfo;

public interface MatchServer {
    SnapInfo draw(Long userId);
}
