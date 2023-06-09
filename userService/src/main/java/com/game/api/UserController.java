package com.game.api;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.game.domain.user.CustomUser;
import com.game.message.RegisterInfo;
import com.game.message.UserInfo;
import com.game.service.CustomUserService;
import com.game.utils.DateUtil;
import com.game.utils.RequestUtil;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseToken;

import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

	private final FirebaseAuth firebaseAuth;
	private final CustomUserService userService;

	@ApiOperation(value = "토큰 추출해서 사용자 등록")
	@PostMapping("/register")
	public UserInfo register(@RequestHeader("Authorization") String authorization,
		@RequestBody RegisterInfo registerInfo) {
		FirebaseToken decodedToken;
		try {
			// Token 추출
			String token = RequestUtil.getAuthorizationToken(authorization);
			decodedToken = firebaseAuth.verifyIdToken(token);
		} catch (IllegalArgumentException | FirebaseAuthException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
				"{\"code\":\"INVALID_TOKEN\", \"message\":\"" + e.getMessage() + "\"}");
		}
		CustomUser registeredUser = userService.createUser(
			decodedToken.getUid(), DateUtil.convertDateFormat(registerInfo.getBirthDate())
		);

		Long userId = registeredUser.getId();
		userService.updateProfileImage(userId);
		return new UserInfo(registeredUser);
	}

	@ApiOperation(value = "개인 정보 조회")
	@GetMapping("/myInfo")
	public UserInfo getMyInfo(Authentication authentication) {
		CustomUser customUser = ((CustomUser)authentication.getPrincipal());
		return new UserInfo(customUser);
	}

	// @ApiOperation(value = "생년월일을 기준으로 DALI에 프로필 사진 요청")
	// @PostMapping("/profile-update")
	// public String updateProfileImage(Authentication authentication) {
	// 	System.out.println("================ profile-update 실행 ================");
	// 	System.out.println("Authentication : " + authentication);
	//
	// 	Object principal = authentication.getPrincipal();
	// 	System.out.println("principal: " + principal.toString());
	// 	System.out.println("id: " + ((CustomUser)principal).getId());
	// 	Long id = ((CustomUser)authentication.getPrincipal()).getId();
	//
	// 	return userService.updateProfileImage(id);
	// }

	@ApiOperation(value = "생년월일을 기준으로 DALI에 프로필 사진 요청")
	@PostMapping("/profile-update")
	public String updateProfileImage(@RequestHeader("Authorization") String authorization) {
		System.out.println("================ profile-update 실행 ================");
		FirebaseToken decodedToken;
		try {
			// Token 추출
			String token = RequestUtil.getAuthorizationToken(authorization);
			decodedToken = firebaseAuth.verifyIdToken(token);
			System.out.println("token 추출: " + userService.findByUid(decodedToken.getUid()).toString());
			Long userId = userService.findByUid(decodedToken.getUid()).getId();
			return userService.updateProfileImage(userId);
		} catch (IllegalArgumentException | FirebaseAuthException e) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
				"{\"code\":\"INVALID_TOKEN\", \"message\":\"" + e.getMessage() + "\"}");
		}
	}

	@ApiOperation(value = "uid 입력 시 userId(pk) 반환")
	@GetMapping
	public Long findByUserId(@RequestParam String uid) {
		log.info("====== findByUserId 호출 (uid: " + uid + ")");
		return userService.findByUid(uid).getId();
	}
}
