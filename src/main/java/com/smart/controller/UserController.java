package com.smart.controller;

import com.smart.dao.ContactRepository;
import com.smart.dao.UserRepository;
import com.smart.helper.Message;
import com.smart.models.Contact;
import com.smart.models.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
import javax.transaction.Transactional;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;

    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;

    //----------------Common method to add data-----------------
    @ModelAttribute
    public void addCommonData(Model model, Principal principal){
        String userName = principal.getName();

        User user = userRepository.getUserByUserName(userName);
        model.addAttribute("user", user);
    }


    //------------------User Dashboard Handler-------------------
    @RequestMapping("/index")
    public String dashboard(Model model, Principal principal){
        model.addAttribute("title","Home - Smart Contact Manager");
        return "normal/user_dashboard";
    }

    //------------------Add form Handler-------------------
    @GetMapping("/add-contact")
    public String openAddContactForm(Model model){
        model.addAttribute("title","Add Contact - Smart Contact Manager");
        model.addAttribute("contact", new Contact());
        return "normal/add_contact_form";
    }

    //------------------Process Add Contact Handler-------------------
    @PostMapping("/process-contact")
    public String processContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                 Principal principal, HttpSession session){
        try {
            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);

            if(file.isEmpty())
            {
                contact.setImage("contact.png");
            }else {
                contact.setImage(file.getOriginalFilename());
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            contact.setUser(user);
            user.getContacts().add(contact);
            this.userRepository.save(user);

            //---------Adding Success message--------
            session.setAttribute("message",new Message("Your contact is added!! Add more...","success"));

        }catch (Exception e){
            System.out.println("Error" + e.getMessage());
            e.printStackTrace();

            //---------Adding Failure message----------
            session.setAttribute("message",new Message("Something Went wrong!! Try again...","danger"));
        }
        return "normal/add_contact_form";
    }


    //------------------Show Contacts Handler-------------------
    @GetMapping("/show-contacts/{page}")
    public String showContacts(@PathVariable Integer page,Model model, Principal principal){
        model.addAttribute("title","Show Contact - Smart Contact Manager");

        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);
        Pageable pageable = PageRequest.of(page, 5);
        Page<Contact> contacts = this.contactRepository.findContactsByUser(user.getId(), pageable);
        model.addAttribute("contacts", contacts);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", contacts.getTotalPages());

        return "normal/show_contacts";
    }

    //------------------Showing Specific Contact Handler-------------------
    @RequestMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId") Integer cId, Model model, Principal principal){
        Optional<Contact> contactOptional = this.contactRepository.findById(cId);
        Contact contact = contactOptional.get();

        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);

        if(user.getId() == contact.getUser().getId()) {
            model.addAttribute("contact", contact);
            model.addAttribute("title",contact.getName());
        }
        return "normal/contact_detail";
    }

    //------------------Delete Contact Handler-------------------
    @GetMapping("/delete/{cid}")
    @Transactional
    public String deleteContact(@PathVariable("cid") Integer cId, Model model, Principal principal, HttpSession session){
        Optional<Contact> contactOptional = this.contactRepository.findById(cId);
        Contact contact = contactOptional.get();

        String userName = principal.getName();
        User user = this.userRepository.getUserByUserName(userName);

        if(user.getId() == contact.getUser().getId()) {
            contact.setUser(null);
            this.contactRepository.delete(contact);
            session.setAttribute("message", new Message("Contact deleted successfully...","success"));
        }
        else{
            session.setAttribute("message", new Message("Something Went Wrong...","danger"));
        }

        return "redirect:/user/show-contacts/0";
    }

    //------------------Open Update form Handler-------------------
    @PostMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model model){
        model.addAttribute("title","Update Contact - Smart Contact Manager");

        Contact contact = this.contactRepository.findById(cid).get();
        model.addAttribute("contact",contact);
        return "normal/update_form";
    }

    //------------------Update Contact Handler-------------------
    @PostMapping("/process-update")
    public String updateContact(@ModelAttribute Contact contact, @RequestParam("profileImage") MultipartFile file,
                                Model model, HttpSession session, Principal principal){

        try {

            String name = principal.getName();
            User user = this.userRepository.getUserByUserName(name);
            Contact oldContactDetail = this.contactRepository.findById(contact.getcId()).get();

            if(file.isEmpty())
            {
                contact.setImage(oldContactDetail.getImage());
            }else {
                //---------Delete old Photo------------
                File deleteFile = new ClassPathResource("static/img").getFile();
                File file1 = new File(deleteFile, oldContactDetail.getImage());
                file1.delete();


                //---------update new photo-------
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath() + File.separator + file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
                contact.setImage(file.getOriginalFilename());
            }
            contact.setUser(user);
            this.contactRepository.save(contact);

            //---------Adding Success message--------
            session.setAttribute("message",new Message("Your contact is Updated!!!","success"));

        }catch (Exception e){
            System.out.println("Error" + e.getMessage());
            e.printStackTrace();

            //---------Adding Failure message----------
            session.setAttribute("message",new Message("Something Went wrong!! Try again...","danger"));
        }
        return "redirect:/user/" + contact.getcId() + "/contact";
    }

    //------------------Your Profile Handler-------------------
    @GetMapping("/profile")
    public String yourProfile(Model model){
        model.addAttribute("title","Profile - Smart Contact Manager");
        return "normal/profile";
    }

    //------------------Open Settings Handler-------------------
    @GetMapping("/settings")
    public String openSettings(){
        return "normal/settings";
    }

    //------------------Change Password Handler-------------------
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword, @RequestParam("newPassword") String newPassword,
                                 Principal principal, HttpSession session){
        User currentUser = this.userRepository.getUserByUserName(principal.getName());

        if(this.bCryptPasswordEncoder.matches(oldPassword, currentUser.getPassword()))
        {
            currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
            this.userRepository.save(currentUser);
            session.setAttribute("message",new Message("Your password is successfully changed","success"));
        }else{
            session.setAttribute("message",new Message("Please Enter correct old password!!!","danger"));
            return "redirect:/user/settings";
        }
        return "redirect:/user/index";
    }
}
